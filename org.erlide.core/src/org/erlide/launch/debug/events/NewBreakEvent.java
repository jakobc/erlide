package org.erlide.launch.debug.events;

import org.erlide.launch.debug.model.ErlangDebugTarget;

import com.ericsson.otp.erlang.OtpErlangObject;

public class NewBreakEvent extends IntEvent {

    public NewBreakEvent(final OtpErlangObject[] cmds) {
        super(cmds);
    }

    @Override
    public void execute(final ErlangDebugTarget debugTarget) {
        // TODO Auto-generated method stub

    }

}
