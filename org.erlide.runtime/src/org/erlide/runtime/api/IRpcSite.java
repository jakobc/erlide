package org.erlide.runtime.api;

import org.erlide.runtime.rpc.IRpcCallback;
import org.erlide.runtime.rpc.IRpcFuture;
import org.erlide.runtime.rpc.IRpcResultCallback;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.runtime.rpc.RpcResult;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpMbox;

public interface IRpcSite {

    /**
     * typed RPC
     * 
     */
    RpcResult call_noexception(final String m, final String f,
            final String signature, final Object... a);

    /**
     * typed RPC with timeout
     * 
     * @throws ConversionException
     */
    RpcResult call_noexception(final long timeout, final String m,
            final String f, final String signature, final Object... args);

    IRpcFuture async_call(final String m, final String f,
            final String signature, final Object... args) throws RpcException;

    IRpcFuture async_call(final OtpErlangObject gleader, final String m,
            final String f, final String signature, final Object... args)
            throws RpcException;

    void async_call_cb(final IRpcCallback cb, final String m, final String f,
            final String signature, final Object... args) throws RpcException;

    void async_call_cb(final IRpcCallback cb, final long timeout,
            final String m, final String f, final String signature,
            final Object... args) throws RpcException;

    void async_call_cb(final IRpcCallback cb, final long timeout,
            final OtpErlangObject gleader, final String m, final String f,
            final String signature, final Object... args) throws RpcException;

    /**
     * Make a RPC but don't wait for any result.
     */
    void cast(final String m, final String f, final String signature,
            final Object... args) throws RpcException;

    /**
     * Make a RPC but don't wait for any result.
     */
    void cast(final OtpErlangObject gleader, final String m, final String f,
            final String signature, final Object... args) throws RpcException;

    /**
     * Make a regular RPC to the given node, with the given arguments.
     */
    OtpErlangObject call(final String m, final String f,
            final String signature, final Object... a) throws RpcException;

    /**
     * Make a regular RPC to the given node, with the given arguments.
     */
    OtpErlangObject call(final long timeout, final String m, final String f,
            final String signature, final Object... a) throws RpcException;

    /**
     * Make a regular RPC to the given node, with the given arguments.
     */
    OtpErlangObject call(final long timeout, final OtpErlangObject gleader,
            final String m, final String f, final String signature,
            final Object... a) throws RpcException;

    /**
     * Calls a function that supports sending progress reports back. The first
     * argument is implicit and is the pid where the reports are to be sent.
     */
    void async_call_result(final IRpcResultCallback cb, final String m,
            final String f, final String signature, final Object... args)
            throws RpcException;

    /**
     * Convenience method to send a remote message.
     */
    void send(final OtpErlangPid pid, final Object msg);

    /**
     * Convenience method to send a remote message.
     */
    void send(final String name, final Object msg);

    /**
     * Convenience method to send a remote message.
     */
    void send(final String fullNodeName, final String name, final Object msg);

    OtpErlangObject getRpcResult(OtpMbox mbox, long timeout, String env)
            throws RpcException;

    void setConnected(boolean b);

}
