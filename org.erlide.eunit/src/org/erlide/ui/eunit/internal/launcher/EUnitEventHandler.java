package org.erlide.ui.eunit.internal.launcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.xtext.xbase.lib.Pair;
import org.erlide.backend.api.IBackend;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.EUnitTestFunction;
import org.erlide.runtime.events.ErlangEventHandler;
import org.erlide.ui.eunit.internal.model.ITestRunListener2;
import org.erlide.util.ErlLogger;
import org.erlide.util.Util;
import org.osgi.service.event.Event;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.collect.Maps;

public class EUnitEventHandler extends ErlangEventHandler {

    private final OtpErlangPid eventPid;
    private final ILaunch launch;
    private final ListenerList /* <ITestRunListener2> */listeners = new ListenerList();
    private final Map<String, Integer> testCounts;
    private final int totalTestCount;

    public EUnitEventHandler(final OtpErlangPid eventPid, final ILaunch launch,
            final IBackend backend,
            final Collection<EUnitTestFunction> testElements,
            final Collection<Integer> testCounts) {
        super("eunit", backend.getName());
        final Pair<Map<String, Integer>, Integer> p = getTestCounts(
                testElements, testCounts);
        this.testCounts = p.getKey();
        totalTestCount = p.getValue();
        ErlLogger.debug(
                "adding eventhandler to eventPid %s launch %s backend %s",
                eventPid, launch, backend);
        this.eventPid = eventPid;
        this.launch = launch;
        EUnitPlugin.getModel().addEventHandler(this);
    }

    private Pair<Map<String, Integer>, Integer> getTestCounts(
            final Collection<EUnitTestFunction> testElements,
            final Collection<Integer> testCounts) {
        final Iterator<EUnitTestFunction> i = testElements.iterator();
        final Iterator<Integer> ic = testCounts.iterator();
        final Map<String, Integer> result = Maps
                .newHashMapWithExpectedSize(testElements.size());
        int total = 0;
        while (i.hasNext() && ic.hasNext()) {
            final Integer n = ic.next();
            total += n;
            result.put(i.next().getName(), n);
        }
        return new Pair<Map<String, Integer>, Integer>(result, total);
    }

    private enum EUnitMsgWhat {
        test_begin, test_end, test_cancel, group_begin, group_end, group_cancel, terminated, run_started;

        static Set<String> allNames() {
            final EUnitMsgWhat[] values = values();
            final HashSet<String> result = new HashSet<String>(values.length);
            for (final EUnitMsgWhat value : values) {
                result.add(value.name());
            }
            return result;
        }
    }

    private interface AllListeners {
        void apply(ITestRunListener2 listener);
    }

    private void allListeners(final AllListeners application) {
        for (final Object listener : listeners.getListeners()) {
            application.apply((ITestRunListener2) listener);
        }
    }

    Set<String> allMsgWhats = EUnitMsgWhat.allNames();

    protected void doHandleMsg(final OtpErlangObject msg) throws Exception {
        ErlLogger.debug("EUnitEventHandler %s", msg);
        if (msg instanceof OtpErlangTuple) {
            final Pair<EUnitMsgWhat, OtpErlangTuple> eunitMsg = getEUnitMsg(msg);
            if (eunitMsg == null) {
                ErlLogger.error("EUnitEventHandler unknown msg '%s'", msg);
                return;
            }
            final EUnitMsgWhat what = eunitMsg.getKey();
            final OtpErlangTuple argument = eunitMsg.getValue();
            AllListeners al = null;
            switch (what) {
            case group_begin:
                al = new AllListeners() {

                    public void apply(final ITestRunListener2 listener) {
                        final String name = Util.stringValue(argument
                                .elementAt(1));
                        final String id = name; // FIXME
                        listener.testTreeEntry(id);
                        listener.testStarted(id, name);
                    }
                };
                break;
            case group_end:
                al = new AllListeners() {

                    public void apply(final ITestRunListener2 listener) {
                        final String name = Util.stringValue(argument
                                .elementAt(1));
                        final String id = name; // FIXME
                        listener.testEnded(id, name);
                    }
                };
                break;
            case group_cancel:
                al = new AllListeners() {

                    public void apply(final ITestRunListener2 listener) {
                        final String name = Util.stringValue(argument
                                .elementAt(1));
                        final String id = name; // FIXME
                        listener.testEnded(id, name);
                    }
                };
                break;
            case test_begin:
                al = new AllListeners() {

                    public void apply(final ITestRunListener2 listener) {
                        final String testName = Util.stringValue(argument
                                .elementAt(0));
                        listener.testTreeEntry(testName);
                        listener.testStarted(testName, testName);
                    }
                };
                break;
            case test_end:
                al = new AllListeners() {
                    public void apply(final ITestRunListener2 listener) {
                        final String name = Util.stringValue(argument
                                .elementAt(1));
                        final String id = name; // FIXME
                        final OtpErlangObject testResult = argument
                                .elementAt(3);
                        if (Util.isOk(testResult)) {
                            listener.testEnded(id, name);
                        } else {
                            final OtpErlangTuple failureT = (OtpErlangTuple) testResult;
                            final String expected = getValueExpected(
                                    testResult, "expected");
                            final String value = getValueExpected(testResult,
                                    "value");
                            listener.testFailed(
                                    ITestRunListener2.STATUS_FAILURE, id, name,
                                    failureT.elementAt(1).toString(), expected,
                                    value);
                        }
                    }
                };
                break;
            case test_cancel:
                al = new AllListeners() {
                    public void apply(final ITestRunListener2 listener) {
                        final String name = Util.stringValue(argument
                                .elementAt(1));
                        final String id = name; // FIXME
                        listener.testEnded(id, name);
                    }
                };
                break;
            case run_started:
                al = new AllListeners() {
                    public void apply(final ITestRunListener2 listener) {
                        listener.testRunStarted(totalTestCount);
                    }
                };
                break;
            case terminated:
                al = new AllListeners() {
                    public void apply(final ITestRunListener2 listener) {
                        listener.testRunEnded(0);
                    }
                };
                break;
            }
            if (al != null) {
                allListeners(al);
            }
        }
    }

    protected static String getValueExpected(final OtpErlangObject testResult,
            final String what) {
        final OtpErlangTuple t = (OtpErlangTuple) testResult;
        final OtpErlangTuple t2 = (OtpErlangTuple) t.elementAt(1);
        final OtpErlangTuple t3 = (OtpErlangTuple) t2.elementAt(1);
        final OtpErlangList l = (OtpErlangList) t3.elementAt(1);
        for (final OtpErlangObject i : l) {
            if (i instanceof OtpErlangTuple) {
                final OtpErlangTuple et = (OtpErlangTuple) i;
                final OtpErlangObject o = et.elementAt(0);
                if (o instanceof OtpErlangAtom) {
                    final OtpErlangAtom whatA = (OtpErlangAtom) o;
                    if (whatA.atomValue().equals(what)) {
                        return et.elementAt(1).toString();
                    }
                }
            }
        }
        return what;
    }

    private Pair<EUnitMsgWhat, OtpErlangTuple> getEUnitMsg(
            final OtpErlangObject msg) {
        if (!(msg instanceof OtpErlangTuple)) {
            return null;
        }
        final OtpErlangTuple t = (OtpErlangTuple) msg;
        if (t.arity() != 3) {
            return null;
        }
        final OtpErlangPid jpid = (OtpErlangPid) t.elementAt(1);
        if (!jpid.equals(eventPid)) {
            return null;
        }
        final OtpErlangAtom whatA = (OtpErlangAtom) t.elementAt(0);
        final String what = whatA.atomValue();
        if (!allMsgWhats.contains(what)) {
            return null;
        }
        final OtpErlangTuple argumentT = (OtpErlangTuple) t.elementAt(2);
        return new Pair<EUnitMsgWhat, OtpErlangTuple>(
                EUnitMsgWhat.valueOf(what), argumentT);
    }

    public ILaunch getLaunch() {
        return launch;
    }

    public void addListener(final ITestRunListener2 listener) {
        listeners.add(listener);
    }

    public void removeListener(final ITestRunListener2 listener) {
        listeners.remove(listener);
    }

    public void shutdown() {
        listeners.clear();
        // backend.getEventDaemon().removeHandler(this);
        // FIXME när ska göras dispose?
    }

    public void handleEvent(final Event event) {
        final OtpErlangObject msg = (OtpErlangObject) event.getProperty("DATA");
        try {
            doHandleMsg(msg);
        } catch (final Exception e) {
            ErlLogger.warn(e);
        }
    }
}
