package jp.sharelock.net

@groovy.transform.CompileStatic
/**
 * Methods related to Networking (not related to NetworkInterface)
 */
class Network {
    /**
     * Get range of IP addresses based in StringE range
     * http://stackoverflow.com/questions/31386323/
     * @param ipRange
     * @return
     */
    static ArrayList<Inet4Address> getIpsFromRange(final String ipRange) { //117.211.141-147.20-218
        String[] segments = ipRange.split("\\.")    //split into 4 segments
        int seg1Lower
        int seg1Upper
        int seg2Lower
        int seg2Upper
        int seg3Lower
        int seg3Upper
        int seg4Lower
        int seg4Upper

        // get lower and upper bound of 1st segment
        String[] seg1 = segments[0].split("-")
        if (seg1.length == 1) {
            seg1Lower = Integer.parseInt(seg1[0])
            seg1Upper = Integer.parseInt(seg1[0])
        } else {
            seg1Lower = Integer.parseInt(seg1[0])
            seg1Upper = Integer.parseInt(seg1[1])
        }

        // get lower and upper bound of 2nd segment
        String[] seg2 = segments[1].split("-")
        if (seg2.length == 1) {
            seg2Lower = Integer.parseInt(seg2[0])
            seg2Upper = Integer.parseInt(seg2[0])
        } else {
            seg2Lower = Integer.parseInt(seg2[0])
            seg2Upper = Integer.parseInt(seg2[1])
        }

        // get lower and upper bound of 3rd segment
        String[] seg3 = segments[2].split("-")
        if (seg3.length == 1) {
            seg3Lower = Integer.parseInt(seg3[0])
            seg3Upper = Integer.parseInt(seg3[0])
        } else {
            seg3Lower = Integer.parseInt(seg3[0])
            seg3Upper = Integer.parseInt(seg3[1])
        }

        // get lower and upper bound of 4th segment
        String[] seg4 = segments[3].split("-")
        if (seg4.length == 1) {
            seg4Lower = Integer.parseInt(seg4[0])
            seg4Upper = Integer.parseInt(seg4[0])
        } else {
            seg4Lower = Integer.parseInt(seg4[0])
            seg4Upper = Integer.parseInt(seg4[1])
        }

        //generate all IPs
        def IPs = new ArrayList<>()
        for (int i = seg1Lower; i <= seg1Upper; i++) {
            for (int j = seg2Lower; j <= seg2Upper; j++) {
                for (int k = seg3Lower; k <= seg3Upper; k++) {
                    for (int l = seg4Lower; l <= seg4Upper; l++) {
                        IPs.add(InetAddress.getByName(i + "." + j + "." + k + "." + l))
                    }
                }
            }
        }

        return IPs
    }
}
