/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

import org.eclipse.titan.runtime.core.Base_Template.template_sel;
import org.eclipse.titan.runtime.core.TitanLoggerApi.DefaultEnd;
import org.eclipse.titan.runtime.core.TitanLoggerApi.DefaultOp;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Dualface__mapped;
import org.eclipse.titan.runtime.core.TitanLoggerApi.FunctionEvent_choice_random;
import org.eclipse.titan.runtime.core.TitanLoggerApi.MatchingFailureType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.MatchingProblemType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.MatchingSuccessType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.MatchingTimeout;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Msg__port__recv;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Msg__port__send;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Port__Misc;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Port__Queue;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Port__State;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Proc__port__in;
import org.eclipse.titan.runtime.core.TitanLoggerApi.Proc__port__out;
import org.eclipse.titan.runtime.core.TitanLoggerApi.QualifiedName;
import org.eclipse.titan.runtime.core.TitanLoggerApi.SetVerdictType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.StatisticsType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.TestcaseType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.TimerGuardType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.TimerType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.TimestampType;
import org.eclipse.titan.runtime.core.TitanLoggerApi.TitanLogEvent;
import org.eclipse.titan.runtime.core.TitanLoggerApi.PortType.enum_type;
import org.eclipse.titan.runtime.core.TitanVerdictType.VerdictTypeEnum;
import org.eclipse.titan.runtime.core.TtcnLogger.Severity;

/**
 * The logger plugin manager, is responsible for managing all the runtime registered logger plug-ins
 *
 * FIXME lots to implement here, this is under construction right now
 *
 * @author Kristof Szabados
 */
public class LoggerPluginManager {
	private static class log_event_struct {
		StringBuilder buffer;
		Severity severity;
		//event_destination, etc...
	}

	private static log_event_struct current_event = null;
	private static Stack<log_event_struct> events = new Stack<log_event_struct>();

	private ArrayList<ILoggerPlugin> plugins_ = new ArrayList<ILoggerPlugin>();

	public LoggerPluginManager() {
		plugins_.add(new LegacyLogger());
	}

	/**
	 * The internal logging function representing the interface between the logger and the loggerPluginManager.
	 * 
	 * log(const API::TitanLogEvent& event) in the LoggerPluginManager
	 * not yet using the event objects to save on complexity and runtime cost.
	 *
	 * quickly becoming deprecated
	 * */
	private void log(final TitanLoggerApi.TitanLogEvent event) {
		//FIXME more complicated
		internal_log_to_all(event, false, false, false);
	}

	/**
	 * The internal logging function of the LoggerPluginManager towards the plugins themselves.
	 *
	 * This will be sending of the event to be logged to the logger plugins later,
	 * Right now we only have one (legacy) logger simulated within this same class.
	 * */
	private void internal_log_to_all(final TitanLoggerApi.TitanLogEvent event, final boolean log_buffered, final boolean separate_file, final boolean use_emergency_mask) {
		for (int i = 0; i < plugins_.size(); i++) {
			plugins_.get(i).log(event, log_buffered, separate_file, use_emergency_mask);
		}
	}

	public void begin_event(final Severity msg_severity) {
		current_event = new log_event_struct();
		current_event.severity = msg_severity;
		current_event.buffer = new StringBuilder(100);
		events.push(current_event);
	}

	public void begin_event_log2str() {
		begin_event(Severity.USER_UNQUALIFIED);//and true
	}

	public void end_event() {
		if (current_event != null) {
			//TODO temporary solution for filtering
			if (TtcnLogger.log_this_event(current_event.severity)) {
				log_unhandled_event(current_event.severity, current_event.buffer.toString());
			}

			events.pop();
			if (!events.isEmpty()) {
				current_event = events.peek();
			} else {
				current_event = null;
			}
		}
	}

	public TitanCharString end_event_log2str() {
		if (current_event != null) {
			final TitanCharString ret_val = new TitanCharString(current_event.buffer);

			events.pop();
			if (!events.isEmpty()) {
				current_event = events.peek();
			} else {
				current_event = null;
			}

			return ret_val;
		}

		return new TitanCharString();
	}

	public void finish_event() {
		if (current_event != null) {
			log_event_str("<unfinished>");
			end_event();
		}
	}

	public void log_event_str( final String string ) {
		if (current_event != null) {
			current_event.buffer.append(string);
		}
	}

	public void log_char(final char c) {
		// TODO: correct log_char
		if (current_event != null) {
			current_event.buffer.append(c);
		}
	}

	public void log_event_va_list(final String formatString, final Object... args) {
		if (current_event != null) {
			current_event.buffer.append(String.format(Locale.US, formatString, args));
		}
	}

	public void log_unhandled_event(final Severity severity, final String message) {
		if (!TtcnLogger.log_this_event(severity) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, severity);
		event.getLogEvent().getChoice().getUnhandledEvent().assign(message);

		log(event);
	}

	public void log_timer_read(final String timer_name, final double timeout_val) {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_READ) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_READ);
		final TimerType timer = event.getLogEvent().getChoice().getTimerEvent().getChoice().getReadTimer();
		timer.getName().assign(timer_name);
		timer.getValue__().assign(timeout_val);

		log(event);
	}

	public void log_timer_start(final String timer_name, final double start_val) {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_START) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_START);
		final TimerType timer = event.getLogEvent().getChoice().getTimerEvent().getChoice().getStartTimer();
		timer.getName().assign(timer_name);
		timer.getValue__().assign(start_val);

		log(event);
	}

	public void log_timer_guard(final double start_val) {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_GUARD) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_GUARD);
		final TimerGuardType timer = event.getLogEvent().getChoice().getTimerEvent().getChoice().getGuardTimer();
		timer.getValue__().assign(start_val);

		log(event);
	}

	public void log_timer_stop(final String timer_name, final double stop_val) {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_STOP) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_STOP);
		final TimerType timer = event.getLogEvent().getChoice().getTimerEvent().getChoice().getStopTimer();
		timer.getName().assign(timer_name);
		timer.getValue__().assign(stop_val);

		log(event);
	}

	public void log_timer_timeout(final String timer_name, final double timeout_val) {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_TIMEOUT) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_TIMEOUT);
		final TimerType timer = event.getLogEvent().getChoice().getTimerEvent().getChoice().getTimeoutTimer();
		timer.getName().assign(timer_name);
		timer.getValue__().assign(timeout_val);

		log(event);
	}

	public void log_timer_any_timeout() {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_TIMEOUT) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_TIMEOUT);
		event.getLogEvent().getChoice().getTimerEvent().getChoice().getTimeoutAnyTimer().assign(TitanNull_Type.NULL_VALUE);

		log(event);
	}

	public void log_timer_unqualified(final String message) {
		if (!TtcnLogger.log_this_event(Severity.TIMEROP_UNQUALIFIED) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TIMEROP_UNQUALIFIED);
		event.getLogEvent().getChoice().getTimerEvent().getChoice().getUnqualifiedTimer().assign(message);

		log(event);
	}

	public void log_matching_timeout(final String timer_name) {
		if (!TtcnLogger.log_this_event(Severity.MATCHING_PROBLEM) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.MATCHING_PROBLEM);
		final MatchingTimeout mt = event.getLogEvent().getChoice().getMatchingEvent().getChoice().getMatchingTimeout();
		if (timer_name != null) {
			mt.getTimer__name().get().assign(timer_name);
		} else {
			mt.getTimer__name().assign(template_sel.OMIT_VALUE);
		}

		log(event);
	}

	public void log_port_queue(final TitanLoggerApi.Port__Queue_operation.enum_type operation, final String port_name, final int componentReference, final int id, final TitanCharString address, final TitanCharString parameter) {
		Severity sev;
		switch (operation) {
		case enqueue__msg:
		case extract__msg:
			sev = Severity.PORTEVENT_MQUEUE;
			break;
		case enqueue__call:
		case enqueue__reply:
		case enqueue__exception:
		case extract__op:
			sev = Severity.PORTEVENT_PQUEUE;
		default:
			throw new TtcnError("Invalid operation");
		}

		if (!TtcnLogger.log_this_event(sev) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, sev);
		final Port__Queue portQueue = event.getLogEvent().getChoice().getPortEvent().getChoice().getPortQueue();
		portQueue.getOperation().assign(operation.ordinal());
		portQueue.getPort__name().assign(port_name);
		portQueue.getCompref().assign(adjust_compref(componentReference));
		portQueue.getMsgid().assign(id);
		portQueue.getAddress__().assign(address);
		portQueue.getParam__().assign(parameter);

		log(event);
	}

	private static int adjust_compref(final int compref) {
		if (compref == TitanComponent.MTC_COMPREF) {
			switch (TTCN_Runtime.get_state()) {
			case MTC_CONTROLPART:
			case SINGLE_CONTROLPART:
				return TitanComponent.CONTROL_COMPREF;
			default:
				break;
			}
		}

		return compref;
	}

	public void log_port_state(final TitanLoggerApi.Port__State_operation.enum_type operation, final String portname) {
		if (!TtcnLogger.log_this_event(Severity.PORTEVENT_STATE)) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.PORTEVENT_STATE);
		final Port__State ps = event.getLogEvent().getChoice().getPortEvent().getChoice().getPortState();
		ps.getOperation().assign(operation);
		ps.getPort__name().assign(portname);

		log(event);
	}

	public void log_procport_send(final String portname, final TitanLoggerApi.Port__oper.enum_type operation, final int componentReference, final TitanCharString system, final TitanCharString parameter) {
		final Severity severity = componentReference == TitanComponent.SYSTEM_COMPREF ? Severity.PORTEVENT_PMOUT : Severity.PORTEVENT_PCOUT;
		if (!TtcnLogger.log_this_event(severity) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, severity);
		final Proc__port__out pt = event.getLogEvent().getChoice().getPortEvent().getChoice().getProcPortSend();
		pt.getOperation().assign(operation);
		pt.getPort__name().assign(portname);
		pt.getCompref().assign(componentReference);
		if (componentReference == TitanComponent.SYSTEM_COMPREF) {
			pt.getSys__name().assign(system);
		}
		pt.getParameter().assign(parameter);

		log(event);
	}

	public void log_procport_recv(final String portname, final TitanLoggerApi.Port__oper.enum_type operation, final int componentReference, final boolean check, final TitanCharString parameter, final int id) {
		final Severity severity = componentReference == TitanComponent.SYSTEM_COMPREF ? Severity.PORTEVENT_PMIN : Severity.PORTEVENT_PCIN;
		if (!TtcnLogger.log_this_event(severity) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, severity);
		final Proc__port__in pt = event.getLogEvent().getChoice().getPortEvent().getChoice().getProcPortRecv();
		pt.getOperation().assign(operation);
		pt.getPort__name().assign(portname);
		pt.getCompref().assign(componentReference);
		pt.getCheck__().assign(check);
		pt.getParameter().assign(parameter);
		pt.getMsgid().assign(id);

		log(event);
	}

	public void log_msgport_send(final String portname, final int componentReference, final TitanCharString parameter) {
		final Severity severity = componentReference == TitanComponent.SYSTEM_COMPREF ? Severity.PORTEVENT_MMSEND : Severity.PORTEVENT_MCSEND;
		if (!TtcnLogger.log_this_event(severity) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, severity);
		final Msg__port__send ms = event.getLogEvent().getChoice().getPortEvent().getChoice().getMsgPortSend();
		ms.getPort__name().assign(portname);
		ms.getCompref().assign(componentReference);
		ms.getParameter().assign(parameter);

		log(event);
	}

	public void log_msgport_recv(final String portname, final TitanLoggerApi.Msg__port__recv_operation.enum_type operation, final int componentReference, final TitanCharString system, final TitanCharString parameter, final int id) {
		final Severity severity = componentReference == TitanComponent.SYSTEM_COMPREF ? Severity.PORTEVENT_MMRECV : Severity.PORTEVENT_MCRECV;
		if (!TtcnLogger.log_this_event(severity) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, severity);
		final Msg__port__recv ms = event.getLogEvent().getChoice().getPortEvent().getChoice().getMsgPortRecv();
		ms.getPort__name().assign(portname);
		ms.getCompref().assign(componentReference);
		if (componentReference == TitanComponent.SYSTEM_COMPREF) {
			ms.getSys__name().assign(system);
		}
		ms.getOperation().assign(operation);
		ms.getMsgid().assign(id);
		ms.getParameter().assign(parameter);

		log(event);
	}

	public void log_dualport_map(final boolean incoming, final String target_type, final TitanCharString value, final int id) {
		final Severity severity = incoming ? Severity.PORTEVENT_DUALRECV : Severity.PORTEVENT_DUALSEND;
		if (!TtcnLogger.log_this_event(severity) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, severity);
		final Dualface__mapped dual = event.getLogEvent().getChoice().getPortEvent().getChoice().getDualMapped();
		dual.getIncoming().assign(incoming);
		dual.getTarget__type().assign(target_type);
		dual.getValue__().assign(value);
		dual.getMsgid().assign(id);

		log(event);
	}

	public void log_setverdict(final VerdictTypeEnum newVerdict, final VerdictTypeEnum oldVerdict, final VerdictTypeEnum localVerdict,
			final String oldReason, final String newReason) {
		if (!TtcnLogger.log_this_event(Severity.VERDICTOP_SETVERDICT) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.VERDICTOP_SETVERDICT);
		final SetVerdictType set = event.getLogEvent().getChoice().getVerdictOp().getChoice().getSetVerdict();
		set.getNewVerdict().assign(newVerdict.ordinal());
		set.getOldVerdict().assign(oldVerdict.ordinal());
		set.getLocalVerdict().assign(localVerdict.ordinal());
		if (oldReason != null) {
			set.getOldReason().get().assign(oldReason);
		} else {
			set.getOldReason().assign(template_sel.OMIT_VALUE);
		}
		if (newReason != null) {
			set.getNewReason().get().assign(newReason);
		} else {
			set.getNewReason().assign(template_sel.OMIT_VALUE);
		}

		log(event);
	}

	private void fill_common_fields(final TitanLogEvent event, final Severity severity) {
		//FIXME implement the rest
		long timestamp = System.currentTimeMillis();
		event.getTimestamp().assign(new TimestampType(new TitanInteger((int)(timestamp / 1000)), new TitanInteger((int)(timestamp % 1000))));

		event.getSeverity().assign(severity.ordinal());
	}

	public void log_testcase_started(final String module_name, final String definition_name ) {
		if (!TtcnLogger.log_this_event(Severity.TESTCASE_START) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TESTCASE_START);
		final QualifiedName qname = event.getLogEvent().getChoice().getTestcaseOp().getChoice().getTestcaseStarted();
		qname.getModule__name().assign(module_name);
		qname.getTestcase__name().assign(definition_name);

		log(event);
	}

	//TODO not yet called from generated code
	public void log_testcase_finished(final String module_name, final String definition_name, final VerdictTypeEnum verdict, final String reason) {
		if (!TtcnLogger.log_this_event(Severity.TESTCASE_FINISH) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.TESTCASE_FINISH);
		final TestcaseType testcase = event.getLogEvent().getChoice().getTestcaseOp().getChoice().getTestcaseFinished();
		final QualifiedName qname = testcase.getName();
		qname.getModule__name().assign(module_name);
		qname.getTestcase__name().assign(definition_name);
		testcase.getVerdict().assign(verdict.ordinal());
		testcase.getReason().assign(reason);

		log(event);
	}

	public void log_controlpart_start_stop(final String moduleName, final boolean finished) {
		if (!TtcnLogger.log_this_event(Severity.STATISTICS_UNQUALIFIED) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.STATISTICS_UNQUALIFIED);
		final StatisticsType stats = event.getLogEvent().getChoice().getStatistics();
		if (finished) {
			stats.getChoice().getControlpartFinish().assign(moduleName);
		} else {
			stats.getChoice().getControlpartStart().assign(moduleName);
		}

		log(event);
	}

	public void log_defaultop_activate(final String name, final int id) {
		if (!TtcnLogger.log_this_event(Severity.DEFAULTOP_ACTIVATE) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, TtcnLogger.Severity.DEFAULTOP_ACTIVATE);
		final DefaultOp defaultop = event.getLogEvent().getChoice().getDefaultEvent().getChoice().getDefaultopActivate();
		defaultop.getName().assign(name);
		defaultop.getId().assign(id);
		defaultop.getEnd().assign(DefaultEnd.enum_type.UNKNOWN_VALUE);

		log(event);
	}


	public void log_matching_problem(final TitanLoggerApi.MatchingProblemType_reason.enum_type reason, final TitanLoggerApi.MatchingProblemType_operation.enum_type operation, final boolean check, final boolean anyport, final String port_name) {
		if (!TtcnLogger.log_this_event(TtcnLogger.Severity.MATCHING_PROBLEM) && (TtcnLogger.get_emergency_logging() <= 0)) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, TtcnLogger.Severity.MATCHING_PROBLEM);
		final MatchingProblemType mp = event.getLogEvent().getChoice().getMatchingEvent().getChoice().getMatchingProblem();
		mp.getReason().assign(reason);
		mp.getAny__port().assign(anyport);
		mp.getCheck__().assign(check);
		mp.getOperation().assign(operation);
		mp.getPort__name().assign(port_name);

		log(event);
	}

	public void log_random(final TitanLoggerApi.RandomAction.enum_type rndAction, final double value, final long seed) {
		if (!TtcnLogger.log_this_event(Severity.FUNCTION_RND) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.FUNCTION_RND);
		final FunctionEvent_choice_random r = event.getLogEvent().getChoice().getFunctionEvent().getChoice().getRandom();
		r.getOperation().assign(rndAction);
		r.getRetval().assign(value);
		r.getIntseed().assign((int)seed);

		log(event);
	}

	public void log_matching_failure(final TitanLoggerApi.PortType.enum_type port_type, final String port_name, final int compref, final TitanLoggerApi.MatchingFailureType_reason.enum_type reason, final TitanCharString info) {
		Severity sev;
		if (compref == TitanComponent.SYSTEM_COMPREF) {
			sev = (port_type == enum_type.message__) ? Severity.MATCHING_MMUNSUCC : Severity.MATCHING_PMUNSUCC;
		} else {
			sev = (port_type == enum_type.message__) ? Severity.MATCHING_MCUNSUCC : Severity.MATCHING_PCUNSUCC;
		}
		if (!TtcnLogger.log_this_event(sev) && (TtcnLogger.get_emergency_logging() <= 0)) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, sev);
		final MatchingFailureType mf = event.getLogEvent().getChoice().getMatchingEvent().getChoice().getMatchingFailure();
		mf.getPort__type().assign(port_type);
		mf.getPort__name().assign(port_name);
		mf.getReason().assign(reason);

		if (compref == TitanComponent.SYSTEM_COMPREF) {
			mf.getChoice().getSystem__();
		} else {
			mf.getChoice().getCompref().assign(compref);
		}

		log(event);
	}

	public void log_matching_success(final TitanLoggerApi.PortType.enum_type port_type, final String port_name, final int compref, final TitanCharString info) {
		Severity sev;
		if(compref == TitanComponent.SYSTEM_COMPREF) {
			sev = port_type == enum_type.message__ ? Severity.MATCHING_MMSUCCESS : Severity.MATCHING_PMSUCCESS;
		} else {
			sev = port_type == enum_type.message__ ? Severity.MATCHING_MCSUCCESS : Severity.MATCHING_PCSUCCESS;
		}

		if(TtcnLogger.log_this_event(sev) && TtcnLogger.get_emergency_logging() <= 0) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, sev);
		final MatchingSuccessType ms = event.getLogEvent().getChoice().getMatchingEvent().getChoice().getMatchingSuccess();
		ms.getPort__type().assign(port_type);
		ms.getPort__name().assign(port_name);

		log(event);
	}

	public void log_port_misc(final TitanLoggerApi.Port__Misc_reason.enum_type reason, final String port_name, final int remote_component, final String remote_port, final String ip_address, final int tcp_port, final int new_size) {
		if (!TtcnLogger.log_this_event(Severity.PORTEVENT_UNQUALIFIED) && (TtcnLogger.get_emergency_logging()<=0)) {
			return;
		}

		final TitanLogEvent event = new TitanLogEvent();
		fill_common_fields(event, Severity.PORTEVENT_UNQUALIFIED);
		final Port__Misc portMisc = event.getLogEvent().getChoice().getPortEvent().getChoice().getPortMisc();
		portMisc.getReason().assign(reason.ordinal());
		portMisc.getPort__name().assign(port_name);
		portMisc.getRemote__component().assign(remote_component);
		portMisc.getRemote__port().assign(remote_port);
		portMisc.getIp__address().assign(ip_address);
		portMisc.getTcp__port().assign(tcp_port);
		portMisc.getNew__size().assign(new_size);

		log(event);
	}
}