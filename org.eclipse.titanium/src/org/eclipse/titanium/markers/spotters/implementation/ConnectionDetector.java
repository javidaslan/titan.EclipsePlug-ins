package org.eclipse.titanium.markers.spotters.implementation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignments;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.TTCN3.statements.Connect_Statement;
import org.eclipse.titan.designer.AST.TTCN3.statements.Port_Utility;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.types.Port_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.TypeSet;
import org.eclipse.titan.designer.AST.TTCN3.types.PortTypeBody.OperationModes;
import org.eclipse.titan.designer.consoles.TITANConsole;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titanium.markers.spotters.BaseModuleCodeSmellSpotter;
import org.eclipse.titanium.markers.types.CodeSmellType;
import org.eclipse.titan.designer.AST.PortReference;

/**
 * 
 * @author jasla
 * This is class is used to detect whether connection is unable or not
 * when connect statement is used.
 */

public class ConnectionDetector extends BaseModuleCodeSmellSpotter {
	private static final String ERROR_MESSAGE = "This connection is problematic -- message from Titanium";
	
	CompilationTimeStamp timestamp = new CompilationTimeStamp(10); 
	
	public ConnectionDetector() {
		super(CodeSmellType.CONNECTION_DETECTOR);
	}

	@Override
	public void process(final IVisitableNode node, final Problems problems) {
		if (node instanceof Connect_Statement) {
			
			final Connect_Statement s = (Connect_Statement) node;
			
			IType portType1;
			IType portType2;
			PortTypeBody body1;
			PortTypeBody body2;

			portType1 = Port_Utility.checkConnectionEndpoint(timestamp, s, s.getComponentReference1(), s.getPortReference1(), false);
			
			if (portType1 == null) {
				body1 = null;
			} else {
				body1 = ((Port_Type) portType1).getPortBody();
			}
			
			portType2 = Port_Utility.checkConnectionEndpoint(timestamp, s, s.getComponentReference2(), s.getPortReference2(), false);
			if (portType2 == null) {
				body2 = null;
			} else {
				body2 = ((Port_Type) portType2).getPortBody();
			}
			
			if ((OperationModes.OP_Message.equals(body1.getOperationMode()) || OperationModes.OP_Mixed.equals(body1.getOperationMode())) && body2.getOutMessage() == null) 
			{
				problems.report(s.getLocation(), s.INCONSISTENTCONNECTION);
			}		
			
		}
	}

	@Override
	public List<Class<? extends IVisitableNode>> getStartNode() {
		final List<Class<? extends IVisitableNode>> ret = new ArrayList<Class<? extends IVisitableNode>>(1);
		ret.add(Connect_Statement.class);
		return ret;
	}
}