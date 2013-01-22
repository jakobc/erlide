package erlang;

import java.util.Collection;
import java.util.List;

import org.erlide.eunit.EUnitTestFunction;
import org.erlide.jinterface.ErlLogger;
import org.erlide.runtime.IRpcSite;
import org.erlide.runtime.rpc.RpcException;
import org.erlide.utils.Util;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangLong;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.collect.Lists;

public final class ErlideEUnit {

	public static List<EUnitTestFunction> findTests(final IRpcSite backend,
			final List<String> beams) {
		OtpErlangObject res = null;
		try {
			res = backend.call("erlide_eunit", "find_tests", "ls", beams);
		} catch (final RpcException e) {
			ErlLogger.warn(e);
		}
		if (Util.isOk(res)) {
			final OtpErlangTuple t = (OtpErlangTuple) res;
			final OtpErlangList l = (OtpErlangList) t.elementAt(1);
			final List<EUnitTestFunction> result = Lists
					.newArrayListWithCapacity(l.arity());
			for (final OtpErlangObject i : l) {
				final OtpErlangTuple funT = (OtpErlangTuple) i;
				result.add(new EUnitTestFunction(funT));
			}
			return result;
		}
		return null;
	}

	public static Collection<Integer> countTests(final IRpcSite backend,
			final List<OtpErlangObject> tuples) {
		final List<Integer> result = Lists.newArrayListWithCapacity(tuples
				.size());
		try {
			final OtpErlangObject res = backend.call("erlide_eunit",
					"count_tests", "lx", tuples);
			if (Util.isOk(res)) {
				final OtpErlangTuple t = (OtpErlangTuple) res;
				final OtpErlangList counts = (OtpErlangList) t.elementAt(1);
				for (final OtpErlangObject o : counts) {
					final OtpErlangTuple t2 = (OtpErlangTuple) o;
					final OtpErlangLong l = (OtpErlangLong) t2.elementAt(0);
					result.add(l.intValue());
				}
			}
		} catch (final Exception e) {
			ErlLogger.warn(e);
		}
		ErlLogger.debug("countTests %s", result);
		return result;
	}

	public static boolean runTests(final IRpcSite backend,
			final OtpErlangList tests, final OtpErlangPid jpid) {
		ErlLogger.debug("erlide_eunit:run_tests %s  (jpid %s", tests, jpid);
		try {
			backend.cast("erlide_eunit", "run_tests", "xx", tests, jpid);
			return true;
		} catch (final RpcException e) {
			ErlLogger.warn(e);
		}
		return false;
	}

}
