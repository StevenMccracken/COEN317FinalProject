package edu.scu.kademlia;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class DataBlock implements Serializable {
    int sampleValue;
}
