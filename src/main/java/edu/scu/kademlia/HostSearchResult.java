package edu.scu.kademlia;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
class HostSearchResult implements Serializable {
    @Getter
    List<Host> nextHost = List.of();
    @Getter
    Optional<DataBlock> data = Optional.empty();

    public HostSearchResult(List<Host> nextHost) {
        this.nextHost = nextHost;
    }

    public HostSearchResult(DataBlock data) {
        this.data = Optional.of(data);
    }
}
