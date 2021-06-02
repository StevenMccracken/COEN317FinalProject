package edu.scu.kademlia;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
class DataBlock implements Serializable {
    int sampleValue;
}
