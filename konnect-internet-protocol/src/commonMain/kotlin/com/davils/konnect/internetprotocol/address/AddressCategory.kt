/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.konnect.internetprotocol.address

/**
 * The purpose of an IP address, as the IANA IPv4 and IPv6 Special-Purpose Address Registries of RFC 6890 and the
 * IANA address space registries assign it. Konnect 1.0.0 follows both special-purpose registries as of 2025-10-09.
 *
 * An [IpAddress] belongs to exactly one category, which [IpAddress.category] returns: the category of the most
 * specific block of the registry that holds the address, or [Public] if no block holds it. A block inside another
 * block wins over it, so `192.0.2.1` is [Documentation], `233.252.0.1` is [Documentation] although it lies in the
 * multicast block, and `255.255.255.255` is [Broadcast] although it lies in the reserved block `240.0.0.0/4`.
 *
 * Whether an address is reachable from the public internet is a separate question that [IpAddress.isPublic]
 * answers; a few addresses of the categories [ProtocolAssignment] and [Translation] are globally reachable as well.
 * Where the registry leaves reachability open, as for [Tunnel], Konnect treats the address as not globally reachable.
 *
 * @since 1.0.0
 */
public enum class AddressCategory {
    /**
     * The unspecified address, `0.0.0.0` of RFC 1122 or `::` of RFC 4291, which stands for the absence of an
     * address and is never the destination of a packet.
     *
     * @since 1.0.0
     */
    Unspecified,

    /**
     * The block `0.0.0.0/8` of RFC 791 for "this host on this network", other than the unspecified address
     * `0.0.0.0` itself.
     *
     * @since 1.0.0
     */
    ThisNetwork,

    /**
     * The loopback addresses, `127.0.0.0/8` of RFC 1122 and `::1` of RFC 4291, which never leave the host.
     *
     * @since 1.0.0
     */
    Loopback,

    /**
     * The private-use blocks of RFC 1918, `10.0.0.0/8`, `172.16.0.0/12` and `192.168.0.0/16`, which may be used
     * freely inside a network but are not routed on the public internet.
     *
     * @since 1.0.0
     */
    Private,

    /**
     * The shared address space `100.64.0.0/10` of RFC 6598, used between a carrier-grade NAT and its customers.
     *
     * @since 1.0.0
     */
    SharedAddressSpace,

    /**
     * The link-local blocks, `169.254.0.0/16` of RFC 3927 and `fe80::/10` of RFC 4291, which are valid only on
     * a single link and never forwarded by a router.
     *
     * @since 1.0.0
     */
    LinkLocal,

    /**
     * The multicast blocks, `224.0.0.0/4` of RFC 5771 and `ff00::/8` of RFC 4291, other than the documentation
     * block `233.252.0.0/24` inside them.
     *
     * @since 1.0.0
     */
    Multicast,

    /**
     * The limited broadcast address `255.255.255.255` of RFC 919 and RFC 8190, which reaches every host on the
     * local network.
     *
     * @since 1.0.0
     */
    Broadcast,

    /**
     * The blocks reserved for examples in documentation: `192.0.2.0/24`, `198.51.100.0/24` and `203.0.113.0/24` of
     * RFC 5737, the multicast block `233.252.0.0/24` of RFC 5771, and `2001:db8::/32` of RFC 3849 and `3fff::/20`
     * of RFC 9637.
     *
     * @since 1.0.0
     */
    Documentation,

    /**
     * The blocks for benchmarking network devices, `198.18.0.0/15` of RFC 2544 and `2001:2::/48` of RFC 5180.
     *
     * @since 1.0.0
     */
    Benchmarking,

    /**
     * The blocks that the IETF assigns to protocols: `192.0.0.0/24` of RFC 6890, `2001::/23` of RFC 2928 and
     * `5f00::/16` of RFC 9602 for SRv6 SIDs, with the blocks inside them, and the protocol blocks outside them.
     *
     * Not globally reachable are, among others, the IPv4 service continuity prefix `192.0.0.0/29` of RFC 7335, the
     * dummy addresses `192.0.0.8` of RFC 7600 and `100:0:0:1::/64` of RFC 9780, the NAT64 discovery addresses
     * `192.0.0.170` and `192.0.0.171` of RFC 7050 and the 6a44 relay anycast address `192.88.99.2` of RFC 6751.
     * Globally reachable are the Port Control Protocol, TURN and DNS-SD SRP anycast addresses `192.0.0.9`,
     * `192.0.0.10`, `2001:1::1`, `2001:1::2` and `2001:1::3`, AMT `192.52.193.0/24` and `2001:3::/32`, AS112
     * `192.31.196.0/24` and `2001:4:112::/48`, the AS112 direct delegation blocks `192.175.48.0/24` and
     * `2620:4f:8000::/48` of RFC 7534, ORCHIDv2 `2001:20::/28` and the drone entity tags `2001:30::/28`.
     *
     * @since 1.0.0
     */
    ProtocolAssignment,

    /**
     * The blocks that are reserved and not in use as unicast space: for IPv4 the formerly class E block
     * `240.0.0.0/4` of RFC 1112 and the deprecated 6to4 relay anycast block `192.88.99.0/24` of RFC 7526; for IPv6
     * every address outside the global unicast space `2000::/3` of RFC 4291 that no more specific block claims.
     *
     * @since 1.0.0
     */
    Reserved,

    /**
     * The unique local IPv6 unicast block `fc00::/7` of RFC 4193, the IPv6 counterpart of [Private].
     *
     * @since 1.0.0
     */
    UniqueLocal,

    /**
     * The IPv4-mapped IPv6 addresses `::ffff:0:0/96` of RFC 4291, which carry an IPv4 address in their last 32 bits.
     *
     * @since 1.0.0
     */
    Ipv4Mapped,

    /**
     * The IPv4/IPv6 translation blocks: the well-known NAT64 prefix `64:ff9b::/96` of RFC 6052, which is globally
     * reachable, and the local-use prefix `64:ff9b:1::/48` of RFC 8215.
     *
     * @since 1.0.0
     */
    Translation,

    /**
     * The discard-only IPv6 block `100::/64` of RFC 6666, whose traffic is dropped.
     *
     * @since 1.0.0
     */
    Discard,

    /**
     * The IPv6 transition tunnels that carry an IPv4 address inside the IPv6 address: Teredo `2001::/32` of RFC
     * 4380 and 6to4 `2002::/16` of RFC 3056.
     *
     * The registry leaves their reachability open, because it depends on the embedded IPv4 address: `2002:7f00:1::`
     * carries the loopback address `127.0.0.1`. Konnect therefore never counts them as globally reachable, so a
     * check with [IpAddress.isPublic] cannot be passed through a tunnel address.
     *
     * @since 1.0.0
     */
    Tunnel,

    /**
     * Every address that no block of the registry holds: an ordinary unicast address of the public internet.
     *
     * @since 1.0.0
     */
    Public,
}
