package org.erlide.engine.services.parsing;

import com.ericsson.otp.erlang.OtpErlangObject;

public interface InternalScanner {

    void create(String module);

    boolean dumpLog(String scannerName, String dumpLocationFilename);

    OtpErlangObject checkAll(String module, String text, boolean getTokens);

}
