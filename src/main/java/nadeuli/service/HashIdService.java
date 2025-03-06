package nadeuli.service;

import org.hashids.Hashids;
import org.springframework.stereotype.Component;

@Component
public class HashIdService {

    private final Hashids hashids;

    // salt, 최소 길이(minHashLength) 등은 원하는 대로 설정
    public HashIdService() {
        this.hashids = new Hashids("nadeuli-salt", 15);
    }

    /**
     * Long ID -> 해시 문자열
     */
    public String encode(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null");
        }
        return hashids.encode(id);
    }

    /**
     * 해시 문자열 -> Long ID
     */
    public Long decode(String hash) {
        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) {
            throw new IllegalArgumentException("Invalid hash: " + hash);
        }
        return decoded[0];
    }
}