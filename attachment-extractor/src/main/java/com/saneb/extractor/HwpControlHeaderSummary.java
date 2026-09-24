package com.saneb.extractor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.TreeMap;

/** 원문·설명·임의 컨트롤 ID 없이 길이와 고정 형식 상태만 집계한다. 추출 판정에는 관여하지 않는다. */
final class HwpControlHeaderSummary {
    private final TreeMap<Key,Integer> counts=new TreeMap<>();
    private record Key(String kind,int bytes,String shape,int tail) implements Comparable<Key> {
        @Override public int compareTo(Key other) {
            int result=kind.compareTo(other.kind);if(result!=0)return result;
            result=Integer.compare(bytes,other.bytes);if(result!=0)return result;
            result=shape.compareTo(other.shape);return result!=0?result:Integer.compare(tail,other.tail);
        }
    }
    void insert(byte[] data) throws IOException {
        int id=data.length<4?0:ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
        String kind=switch(id) {
            case 0x74626c20 -> "TABLE";case 0x73656364 -> "SECTION";case 0x636f6c64 -> "COLUMN";
            case 0x25686c6b -> "HYPERLINK";
            // 공개 ControlType의 고정 ID만 이름으로 치환한다. 지원 여부나 내용 없음 판정이 아니다.
            case 0x67736f20 -> "GSO";case 0x61746e6f -> "AUTO_NUMBER";case 0x6e776e6f -> "NEW_NUMBER";
            case 0x70676864 -> "PAGE_HIDE";case 0x70676374 -> "PAGE_ODD_EVEN";case 0x70676e70 -> "PAGE_NUMBER";
            case 0x68656164 -> "HEADER";case 0x666f6f74 -> "FOOTER";
            case 0x666e2020 -> "FOOTNOTE";case 0x656e2020 -> "ENDNOTE";case 0x65716564 -> "EQUATION";
            case 0x6964786d -> "INDEX_MARK";case 0x626f6b6d -> "BOOKMARK";
            case 0x74637073 -> "OVERLAPPING_LETTER";case 0x74647574 -> "ADDITIONAL_TEXT";
            case 0x74636d74 -> "HIDDEN_COMMENT";case 0x666f726d -> "FORM";case 0x25636c6b -> "CLICK_HERE";
            default -> "OTHER";
        };
        String shape="NOT_TABLE";int tail=0;
        if("TABLE".equals(kind)) {
            if(data.length==40)shape="COMMON_ONLY";
            else if(data.length==44)shape="FIXED_ONLY";
            else if(data.length<46)shape="SHORT";
            else {
                int expected=46+2*Short.toUnsignedInt(ByteBuffer.wrap(data,44,2).order(ByteOrder.LITTLE_ENDIAN).getShort());
                if(expected>data.length)shape="DECLARED_TOO_LONG";
                else {
                    tail=data.length-expected;boolean zero=true;
                    for(int i=expected;i<data.length;i++)if(data[i]!=0){zero=false;break;}
                    shape=tail==0?"EXTENDED_EXACT":zero?"EXTRA_ZERO":"EXTRA_NONZERO";
                }
            }
        }
        Key key=new Key(kind,data.length,shape,tail);
        if(!counts.containsKey(key)&&counts.size()>=128)throw new IOException("LIMIT_EXCEEDED");
        counts.merge(key,1,Math::addExact);
    }
    List<ExtractionResult.ControlHeader> select() {
        return counts.entrySet().stream().map(e->new ExtractionResult.ControlHeader(
                e.getKey().kind,e.getKey().bytes,e.getKey().shape,e.getKey().tail,e.getValue())).toList();
    }
}
