package com.mlyncar.dp.interpreter.core.impl.manager;

import java.util.ListIterator;

import org.eclipse.gmf.runtime.notation.Bounds;
import org.eclipse.gmf.runtime.notation.NotationFactory;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.uml2.uml.ActionExecutionSpecification;
import org.eclipse.uml2.uml.CombinedFragment;
import org.eclipse.uml2.uml.Lifeline;
import org.eclipse.uml2.uml.MessageOccurrenceSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mlyncar.dp.interpreter.exception.ExecSpecNotFoundException;
import com.mlyncar.dp.interpreter.exception.InterpreterException;
import com.mlyncar.dp.transformer.entity.EdgeType;
import com.mlyncar.dp.transformer.entity.Node;

public class NotationBoundsManager {

    private final Logger logger = LoggerFactory.getLogger(NotationBoundsManager.class);
    private NotationManager notationManager;

    public NotationBoundsManager(NotationManager manager) {
        this.notationManager = manager;
    }

    public Integer calculateLifelinePosition() {
        View lifelineCompartment = (View) notationManager.getLifelineCompartment();

        ListIterator<View> listIter = lifelineCompartment.getChildren().listIterator(lifelineCompartment.getChildren().size());
        while (listIter.hasPrevious()) {
            View prev = listIter.previous();
            if (prev.getElement() instanceof Lifeline) {
                Bounds bounds = (Bounds) ((org.eclipse.gmf.runtime.notation.Node) prev).getLayoutConstraint();
                return bounds.getX() + 150;
            }
        }
        logger.debug("Last lifeline not found, new placement for lifeline is 0");
        return 0;
    }

    public void adjustParentExecSpecs(Node nodeToAdjust, int newHeight) throws InterpreterException {
        while (nodeToAdjust.getParentNode() != null) {
            try {
                Bounds boundsStart = getNodeExecutionOccurrenceStartBounds(nodeToAdjust);
                Bounds boundsEnd = getNodeExecutionOccurrenceEndBounds(nodeToAdjust);
                if (boundsStart == null || boundsEnd == null) {
                    return;
                }
                boundsStart.setHeight(boundsStart.getHeight() + newHeight);
                boundsEnd.setHeight(boundsEnd.getHeight() + newHeight);
            } catch (ExecSpecNotFoundException e) {
                logger.debug("Error in adjusting exec specs: " + e.getMessage());
            }
            nodeToAdjust = nodeToAdjust.getParentNode();
        }
    }

    public void moveActionSpecs(int moveReference, int newHeight) {
        View lifelineCompartment = (View) notationManager.getLifelineCompartment();
        for (Object lifelineObj : lifelineCompartment.getChildren()) {
            View lifelineView = (View) lifelineObj;
            org.eclipse.gmf.runtime.notation.Node fragmentComp = (org.eclipse.gmf.runtime.notation.Node) lifelineObj;
            if(lifelineView.getElement() instanceof CombinedFragment) {
            	CombinedFragment fragment = (CombinedFragment) lifelineView.getElement();
                Bounds bounds = (Bounds) fragmentComp.getLayoutConstraint();
                logger.debug("MoveReference {}, Y {}", moveReference, bounds.getY());
            	if (bounds.getY() > moveReference) {
            		logger.debug("Combined fragment {} is below.",fragment.toString());
            		bounds.setY(bounds.getY() + newHeight);
            	}
            }
            for (Object lifelineComponentObj : lifelineView.getChildren()) {
                org.eclipse.gmf.runtime.notation.Node lifelineComponent = (org.eclipse.gmf.runtime.notation.Node) lifelineComponentObj;

                if (lifelineComponent.getElement() instanceof ActionExecutionSpecification) {
                    ActionExecutionSpecification spec = (ActionExecutionSpecification) lifelineComponent.getElement();
                    Bounds bounds = (Bounds) lifelineComponent.getLayoutConstraint();
                    logger.debug("Checking if {} is below.", spec.getName());
                    if (bounds.getY() > moveReference) {
                        logger.debug("{} is below, new Y: {} ", spec.getName(), bounds.getY() + newHeight);
                        bounds.setY(bounds.getY() + newHeight);
                    }
                }
            }
        }
    }

    public Bounds createExecBounds(Node newValue, boolean isEnd) throws InterpreterException {
        boolean hasParent = newValue.getParentNode() != null;
        boolean hasSibling = newValue.getLeftSibling() != null;

        Bounds bounds = NotationFactory.eINSTANCE.createBounds();
        try {
            bounds.setX(getActionExecutionPositionX(newValue, isEnd, hasSibling));
        } catch(ExecSpecNotFoundException ex) {
        	throw new InterpreterException("Unable to set x for new exec spec in message add " + newValue.getCreateEdge().getName(), ex);
        }

        if (!hasSibling && !hasParent) {
            bounds.setY(30);
            bounds.setHeight(50);
            return bounds;
        }
        if (hasSibling) {
            Bounds siblingBounds = getNodeExecutionOccurrenceStartBounds(newValue.getLeftSibling());
            bounds.setY(siblingBounds.getY() + siblingBounds.getHeight() + 30);
        } else {
            Bounds parentBounds = getNodeExecutionOccurrenceStartBounds(newValue.getParentNode());
            if(parentBounds == null) {
            	bounds.setY(30);
            } else {
                bounds.setY(parentBounds.getY() + 30);
            }

        }
        if (isEnd) {
            bounds.setHeight(40);
            bounds.setY(bounds.getY() + 5);
        } else {
            bounds.setHeight(50);
        }
        return bounds;
    }

    public Bounds extractFragmentBounds(Node node, org.eclipse.gmf.runtime.notation.Node lifelineNode) throws InterpreterException {
        Bounds bounds = NotationFactory.eINSTANCE.createBounds();
        org.eclipse.gmf.runtime.notation.Node actionNode = getNodeExecutionNotationStart(node);
        Bounds actionBounds = (Bounds) actionNode.getLayoutConstraint();
        Bounds lifelineBounds = (Bounds) lifelineNode.getLayoutConstraint();
        bounds.setHeight(actionBounds.getHeight() + 20);
        logger.debug("Setting fragment height {}", bounds.getHeight());
        bounds.setY(actionBounds.getY() + lifelineBounds.getY() + 10); //position to be investigated
        logger.debug("Setting fragment y {}", bounds.getY());
        bounds.setX(lifelineBounds.getX() - 50);
        logger.debug("Setting fragment x {}", bounds.getX());
        bounds.setWidth(calculateLifelinePosition() - lifelineBounds.getX());
        logger.debug("Setting fragment width {}", bounds.getWidth());
        return bounds;
    }
    
    public Bounds extractCombinedFragmentBounds(Node node, CombinedFragment combinedFragment) {
	  Bounds bounds = NotationFactory.eINSTANCE.createBounds();
	  org.eclipse.gmf.runtime.notation.Node view = (org.eclipse.gmf.runtime.notation.Node) notationManager.getFragmentView(combinedFragment.getName());
	  Bounds fragmentBounds = (Bounds) view.getLayoutConstraint();
	  
	  bounds.setHeight(fragmentBounds.getHeight() + 30);
	  bounds.setY(fragmentBounds.getY() - 10);
	  bounds.setX(fragmentBounds.getX() - 50);
	  bounds.setWidth(fragmentBounds.getWidth() + 100);
	  return bounds;
    }

    public void updateFragmentSize(org.eclipse.gmf.runtime.notation.Node fragment, Bounds actionBounds) {
    	Bounds bounds = (Bounds) fragment.getLayoutConstraint();
    	bounds.setWidth(calculateLifelinePosition() - bounds.getX());
    	bounds.setHeight(bounds.getHeight() + actionBounds.getHeight() + 20);
    	fragment.setLayoutConstraint(bounds);
    }
    
    private Bounds getNodeExecutionOccurrenceStartBounds(Node refNode) throws InterpreterException {
        org.eclipse.gmf.runtime.notation.Node node = getNodeExecutionNotationStart(refNode);
        if (node == null) {
            return null;
        }
        return (Bounds) node.getLayoutConstraint();
    }

    private Bounds getNodeExecutionOccurrenceEndBounds(Node refNode) throws InterpreterException, ExecSpecNotFoundException {
        org.eclipse.gmf.runtime.notation.Node node = getNodeExecutionNotationEnd(refNode);
        if (node == null) {
            return null;
        }
        return (Bounds) node.getLayoutConstraint();
    }

    private org.eclipse.gmf.runtime.notation.Node getNodeExecutionNotationEnd(Node node) throws ExecSpecNotFoundException, InterpreterException {
        View lifelineView = notationManager.getLifelineView(node.getName());
        if (lifelineView == null) {
            return null;
        }
        logger.debug("Exec spec message to found: {}, on lifeline {} ", node.getCreateEdge().getName(), node.getName());
        boolean firstSelfFound = false;
        for (Object viewObj : lifelineView.getChildren()) {
            View view = (View) viewObj;
            if (view.getElement() != null && view.getElement() instanceof ActionExecutionSpecification) {
                ActionExecutionSpecification specification = (ActionExecutionSpecification) view.getElement();
                if (specification.getStart() instanceof MessageOccurrenceSpecification) {
                    String messageName = ((MessageOccurrenceSpecification) specification.getStart()).getMessage().getName();
                    if (messageName.equals(node.getCreateEdge().getName())) {
                        if (node.getCreateEdge().getEdgeType().equals(EdgeType.SELF) && !firstSelfFound) {
                            firstSelfFound = true;
                            continue;
                        }
                        logger.debug("Found spec {}", specification.getName());
                        return (org.eclipse.gmf.runtime.notation.Node) viewObj;
                    }
                }
            }
        }
        if(node.getLeftSibling() != null) {
        	return getNodeExecutionNotationEnd(node.getLeftSibling());
        } else {
        	return null;
        }
    }

    private org.eclipse.gmf.runtime.notation.Node getNodeExecutionNotationStart(Node node) throws InterpreterException {
        View lifelineView = notationManager.getLifelineView(node.getParentNode().getName());
        if (lifelineView == null) {
            return null;
        }
        for (Object viewObj : lifelineView.getChildren()) {
            View view = (View) viewObj;
            if (view.getElement() != null && view.getElement() instanceof ActionExecutionSpecification) {
                ActionExecutionSpecification specification = (ActionExecutionSpecification) view.getElement();
                if (specification.getStart() instanceof MessageOccurrenceSpecification) {
                    String messageName = ((MessageOccurrenceSpecification) specification.getStart()).getMessage().getName();
                    logger.debug("Comparing spec {}", specification.getName());
                    if (messageName.equals(node.getCreateEdge().getName())) {
                        logger.debug("Found spec {}", specification.getName());
                        return (org.eclipse.gmf.runtime.notation.Node) viewObj;
                    }
                }
            }
        }
        if(node.getLeftSibling()!= null) {
            return getNodeExecutionNotationStart(node.getLeftSibling());
        } else {
            return getNodeExecutionNotationStart(node.getParentNode());
        }
    }

    private Integer getActionExecutionPositionX(Node newValue, boolean isEnd, boolean hasSibling) throws InterpreterException, ExecSpecNotFoundException {
    	logger.debug("Getting execution position x for new message {}", newValue.getCreateEdge().getName());
        if (hasSibling) {
        	int x;
        	logger.debug("Message has sibling, checking for sibling exec specs");
            if (newValue.getCreateEdge().getEdgeType().equals(EdgeType.SELF)) {
                Bounds siblingBounds = getNodeExecutionOccurrenceStartBounds(newValue.getLeftSibling());
                return siblingBounds.getX() + 7;
            }
            if(isEnd) {
                Bounds siblingBounds = getNodeExecutionOccurrenceEndBounds(newValue.getLeftSibling());
                if(siblingBounds != null) {
                    x = siblingBounds.getX();
                    if(newValue.getCreateEdge().getEdgeType().equals(EdgeType.SELF)) {
                    	x += 7;
                    }
                    return x;
                }
            } else {
            	Bounds siblingBounds = getNodeExecutionOccurrenceStartBounds(newValue.getLeftSibling());
                x = siblingBounds.getX();
                return x;
            }

        }
        View lifelineView;
        if (isEnd) {
            lifelineView = notationManager.getLifelineView(newValue.getName());
        } else {
            lifelineView = notationManager.getLifelineView(newValue.getParentNode().getName());
        }
        org.eclipse.gmf.runtime.notation.Node node = (org.eclipse.gmf.runtime.notation.Node) lifelineView;
        Bounds bounds = (Bounds) node.getLayoutConstraint();
        if (newValue.getCreateEdge().getEdgeType().equals(EdgeType.SELF) && isEnd) {
            return bounds.getWidth() / 2;
        }
        return bounds.getWidth() / 2 - 8;
    }
   
}
