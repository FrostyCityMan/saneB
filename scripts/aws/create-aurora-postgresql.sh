#!/usr/bin/env bash
set -euo pipefail

REGION="${REGION:-${AWS_REGION:-${AWS_DEFAULT_REGION:-ap-northeast-2}}}"
PROJECT="${PROJECT:-saneb}"
ENVIRONMENT="${ENVIRONMENT:-prod}"
DB_CLUSTER_ID="${DB_CLUSTER_ID:-saneb-prod-aurora}"
DB_INSTANCE_ID="${DB_INSTANCE_ID:-saneb-prod-aurora-1}"
DB_NAME="${DB_NAME:-saneb}"
DB_MASTER_USERNAME="${DB_MASTER_USERNAME:-saneb_admin}"
DB_SUBNET_GROUP_NAME="${DB_SUBNET_GROUP_NAME:-saneb-prod-aurora-subnet-group}"
DB_SECURITY_GROUP_NAME="${DB_SECURITY_GROUP_NAME:-saneb-prod-aurora-sg}"
MIN_ACU="${MIN_ACU:-0.5}"
MAX_ACU="${MAX_ACU:-2}"
ENGINE_VERSION="${ENGINE_VERSION:-}"
APP_SECURITY_GROUP_ID="${APP_SECURITY_GROUP_ID:-}"
VPC_ID="${VPC_ID:-}"
DELETION_PROTECTION="${DELETION_PROTECTION:-true}"

if [[ -z "$APP_SECURITY_GROUP_ID" ]]; then
  echo "APP_SECURITY_GROUP_ID is required. Set it to the EC2/application security group that can access Aurora." >&2
  exit 1
fi

if [[ -z "$VPC_ID" ]]; then
  VPC_ID="$(
    aws ec2 describe-security-groups \
      --region "$REGION" \
      --group-ids "$APP_SECURITY_GROUP_ID" \
      --query 'SecurityGroups[0].VpcId' \
      --output text
  )"
fi

if [[ -z "$VPC_ID" || "$VPC_ID" == "None" ]]; then
  echo "Unable to resolve VPC_ID from APP_SECURITY_GROUP_ID=$APP_SECURITY_GROUP_ID." >&2
  exit 1
fi

mapfile -t SUBNET_IDS < <(
  aws ec2 describe-subnets \
    --region "$REGION" \
    --filters "Name=vpc-id,Values=$VPC_ID" "Name=state,Values=available" \
    --query 'Subnets[].SubnetId' \
    --output text | tr '\t' '\n'
)

if [[ "${#SUBNET_IDS[@]}" -lt 2 ]]; then
  echo "Aurora requires a DB subnet group across at least two subnets. Found ${#SUBNET_IDS[@]} in $VPC_ID." >&2
  exit 1
fi

AZ_COUNT="$(
  aws ec2 describe-subnets \
    --region "$REGION" \
    --subnet-ids "${SUBNET_IDS[@]}" \
    --query 'Subnets[].AvailabilityZone' \
    --output text | tr '\t' '\n' | sort -u | wc -l | tr -d ' '
)"

if [[ "$AZ_COUNT" -lt 2 ]]; then
  echo "Aurora requires subnets in at least two Availability Zones. Found $AZ_COUNT AZ in $VPC_ID." >&2
  exit 1
fi

if aws rds describe-db-subnet-groups \
  --region "$REGION" \
  --db-subnet-group-name "$DB_SUBNET_GROUP_NAME" >/dev/null 2>&1; then
  echo "DB subnet group exists: $DB_SUBNET_GROUP_NAME"
else
  aws rds create-db-subnet-group \
    --region "$REGION" \
    --db-subnet-group-name "$DB_SUBNET_GROUP_NAME" \
    --db-subnet-group-description "saneB Aurora PostgreSQL subnet group" \
    --subnet-ids "${SUBNET_IDS[@]}" \
    --tags "Key=Project,Value=$PROJECT" "Key=Environment,Value=$ENVIRONMENT"
fi

DB_SECURITY_GROUP_ID="$(
  aws ec2 describe-security-groups \
    --region "$REGION" \
    --filters "Name=vpc-id,Values=$VPC_ID" "Name=group-name,Values=$DB_SECURITY_GROUP_NAME" \
    --query 'SecurityGroups[0].GroupId' \
    --output text
)"

if [[ "$DB_SECURITY_GROUP_ID" == "None" || -z "$DB_SECURITY_GROUP_ID" ]]; then
  DB_SECURITY_GROUP_ID="$(
    aws ec2 create-security-group \
      --region "$REGION" \
      --group-name "$DB_SECURITY_GROUP_NAME" \
      --description "saneB Aurora PostgreSQL access" \
      --vpc-id "$VPC_ID" \
      --tag-specifications "ResourceType=security-group,Tags=[{Key=Project,Value=$PROJECT},{Key=Environment,Value=$ENVIRONMENT}]" \
      --query 'GroupId' \
      --output text
  )"
fi

set +e
aws ec2 authorize-security-group-ingress \
  --region "$REGION" \
  --group-id "$DB_SECURITY_GROUP_ID" \
  --ip-permissions "IpProtocol=tcp,FromPort=5432,ToPort=5432,UserIdGroupPairs=[{GroupId=$APP_SECURITY_GROUP_ID}]" >/tmp/saneb-aurora-ingress.out 2>/tmp/saneb-aurora-ingress.err
INGRESS_STATUS=$?
set -e

if [[ "$INGRESS_STATUS" -ne 0 ]]; then
  if grep -q "InvalidPermission.Duplicate" /tmp/saneb-aurora-ingress.err; then
    echo "Security group ingress already exists: $APP_SECURITY_GROUP_ID -> $DB_SECURITY_GROUP_ID:5432"
  else
    cat /tmp/saneb-aurora-ingress.err >&2
    exit "$INGRESS_STATUS"
  fi
fi

if [[ -z "$ENGINE_VERSION" ]]; then
  ENGINE_VERSION="$(
    aws rds describe-db-engine-versions \
      --region "$REGION" \
      --engine aurora-postgresql \
      --default-only \
      --query 'DBEngineVersions[0].EngineVersion' \
      --output text
  )"
fi

if [[ -z "$ENGINE_VERSION" || "$ENGINE_VERSION" == "None" ]]; then
  echo "Unable to resolve an Aurora PostgreSQL engine version. Set ENGINE_VERSION explicitly." >&2
  exit 1
fi

DELETION_PROTECTION_FLAG="--deletion-protection"
if [[ "$DELETION_PROTECTION" != "true" ]]; then
  DELETION_PROTECTION_FLAG="--no-deletion-protection"
fi

if aws rds describe-db-clusters \
  --region "$REGION" \
  --db-cluster-identifier "$DB_CLUSTER_ID" >/dev/null 2>&1; then
  echo "DB cluster exists: $DB_CLUSTER_ID"
else
  aws rds create-db-cluster \
    --region "$REGION" \
    --db-cluster-identifier "$DB_CLUSTER_ID" \
    --engine aurora-postgresql \
    --engine-version "$ENGINE_VERSION" \
    --database-name "$DB_NAME" \
    --master-username "$DB_MASTER_USERNAME" \
    --manage-master-user-password \
    --storage-encrypted \
    --backup-retention-period 7 \
    --port 5432 \
    --db-subnet-group-name "$DB_SUBNET_GROUP_NAME" \
    --vpc-security-group-ids "$DB_SECURITY_GROUP_ID" \
    --serverless-v2-scaling-configuration "MinCapacity=$MIN_ACU,MaxCapacity=$MAX_ACU" \
    --copy-tags-to-snapshot \
    "$DELETION_PROTECTION_FLAG" \
    --tags "Key=Project,Value=$PROJECT" "Key=Environment,Value=$ENVIRONMENT"
fi

if aws rds describe-db-instances \
  --region "$REGION" \
  --db-instance-identifier "$DB_INSTANCE_ID" >/dev/null 2>&1; then
  echo "DB instance exists: $DB_INSTANCE_ID"
else
  aws rds create-db-instance \
    --region "$REGION" \
    --db-instance-identifier "$DB_INSTANCE_ID" \
    --db-cluster-identifier "$DB_CLUSTER_ID" \
    --engine aurora-postgresql \
    --db-instance-class db.serverless \
    --no-publicly-accessible \
    --tags "Key=Project,Value=$PROJECT" "Key=Environment,Value=$ENVIRONMENT"
fi

aws rds wait db-instance-available \
  --region "$REGION" \
  --db-instance-identifier "$DB_INSTANCE_ID"

aws rds wait db-cluster-available \
  --region "$REGION" \
  --db-cluster-identifier "$DB_CLUSTER_ID"

DB_ENDPOINT="$(
  aws rds describe-db-clusters \
    --region "$REGION" \
    --db-cluster-identifier "$DB_CLUSTER_ID" \
    --query 'DBClusters[0].Endpoint' \
    --output text
)"

SECRET_ARN="$(
  aws rds describe-db-clusters \
    --region "$REGION" \
    --db-cluster-identifier "$DB_CLUSTER_ID" \
    --query 'DBClusters[0].MasterUserSecret.SecretArn' \
    --output text
)"

cat <<EOF

Aurora PostgreSQL is ready.

REGION=$REGION
DB_CLUSTER_ID=$DB_CLUSTER_ID
DB_INSTANCE_ID=$DB_INSTANCE_ID
DB_SECURITY_GROUP_ID=$DB_SECURITY_GROUP_ID
DB_NAME=$DB_NAME
DB_ENDPOINT=$DB_ENDPOINT
MASTER_SECRET_ARN=$SECRET_ARN

Application environment values:
SPRING_PROFILES_ACTIVE=prod
AWS_REGION=$REGION
DB_URL=jdbc:postgresql://$DB_ENDPOINT:5432/$DB_NAME?sslmode=require
DB_USERNAME=$DB_MASTER_USERNAME
DB_SECRET_ARN=$SECRET_ARN

Required EC2 instance role permission:
secretsmanager:GetSecretValue on $SECRET_ARN
EOF
