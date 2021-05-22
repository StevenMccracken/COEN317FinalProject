package edu.scu.kademlia;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor
class HostSearchResult {
    @Getter
    Optional<Host> nextHost = Optional.empty();
    @Getter
    Optional<DataBlock> data = Optional.empty();

    public HostSearchResult(Host nextHost) {
        this.nextHost = Optional.of(nextHost);
    }

    public HostSearchResult(DataBlock data) {
        this.data = Optional.of(data);
    }
}
