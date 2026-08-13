package com.saneb.domain.announcementsource.provider.content;

import java.net.InetAddress;
import java.net.UnknownHostException;

@FunctionalInterface
interface ProviderContentHostResolver {

    InetAddress[] selectAddressList(String host) throws UnknownHostException;
}
