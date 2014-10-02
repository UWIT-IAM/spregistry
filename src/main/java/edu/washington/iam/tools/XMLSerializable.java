package edu.washington.iam.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;

public interface XMLSerializable extends Serializable {
    void writeXml(BufferedWriter xout) throws IOException;
}
