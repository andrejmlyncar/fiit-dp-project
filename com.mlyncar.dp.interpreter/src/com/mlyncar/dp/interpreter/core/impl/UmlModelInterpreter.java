package com.mlyncar.dp.interpreter.core.impl;

import java.io.IOException;

import org.eclipse.uml2.uml.Lifeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mlyncar.dp.comparison.entity.Change;
import com.mlyncar.dp.comparison.entity.ChangeLog;
import com.mlyncar.dp.interpreter.core.impl.manager.ModelManager;
import com.mlyncar.dp.interpreter.core.impl.manager.NotationManager;
import com.mlyncar.dp.interpreter.core.modelset.MessageAddModelSet;
import com.mlyncar.dp.interpreter.core.modelset.MessageRemoveModelSet;
import com.mlyncar.dp.interpreter.exception.InterpreterException;
import com.mlyncar.dp.transformer.entity.EdgeType;
import com.mlyncar.dp.transformer.entity.Node;

public class UmlModelInterpreter extends AbstractInterpreter {

    private final Logger logger = LoggerFactory.getLogger(UmlModelInterpreter.class);
    private final NotationManager notationManager;
    private final ModelManager modelManager;

    public UmlModelInterpreter(ChangeLog changeLog) throws InterpreterException {
        logger.debug("Interpreter diagram " + changeLog.getReferenceGraph().getSeqDiagram().getName());
        this.notationManager = new NotationManager(changeLog);
        this.modelManager = new ModelManager(changeLog);
    }

    @Override
    protected void interpretMessageAdd(Change change) throws InterpreterException {
        if (change.getNewValue().getCreateEdge().getEdgeType().equals(EdgeType.RETURN)) {
            return;
        }

        Node nodeToAdd = change.getNewValue();
        Node nodeToAddReturn = null;
        for (Node node : nodeToAdd.childNodes()) {
            if (node.isReply()) {
                nodeToAddReturn = node;
            }
        }
        if (nodeToAddReturn == null) {
            throw new InterpreterException("Unable to interpret message " + nodeToAdd.getCreateEdge().getName() + " because it does not contain return message");
        }

        MessageAddModelSet modelSet = modelManager.addMessageToModel(nodeToAdd, nodeToAddReturn);
        notationManager.addMessageToNotation(nodeToAdd, modelSet.getNewMessage(), modelSet.getNewReplyMessage(), modelSet.getActionSpecStart(), modelSet.getActionSpecEnd());
        storeModelResource();
        storeNotationResource();
        logger.debug("Message add interpreted to uml and notation model");
    }

    @Override
    protected void interpretLifelineAdd(Change change) throws InterpreterException {
        Lifeline newLifeline = modelManager.addLifelineToModel(change.getNewValue());
        notationManager.addLifelineToNotation(change.getNewValue(), newLifeline);
        storeModelResource();
        storeNotationResource();
        logger.debug("Lifeline add interpreted to uml model");
    }

    @Override
    protected void interpretMessageRemove(Change change) throws InterpreterException {
        if (change.getNewValue().getCreateEdge().getEdgeType().equals(EdgeType.RETURN)) {
            return;
        }
        Node nodeToRemove = change.getNewValue();
        Node nodeToRemoveReturn = null;
        for (Node node : nodeToRemove.childNodes()) {
            if (node.isReply()) {
                nodeToRemoveReturn = node;
            }
        }
        if (nodeToRemoveReturn == null) {
            throw new InterpreterException("Unable to interpret message " + nodeToRemove.getCreateEdge().getName() + " because it does not contain return message");
        }

        MessageRemoveModelSet modelSet = notationManager.removeMessageFromNotation(nodeToRemove, nodeToRemoveReturn, modelManager.getInteraction());
        modelManager.removeMessageFromModel(nodeToRemove, nodeToRemoveReturn, modelSet);
        storeNotationResource();
        storeModelResource();
    }

    @Override
    protected void interpretMessageModify(Change change) throws InterpreterException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void interpretLifelineRemove(Change change) throws InterpreterException {
        notationManager.removeLifelineFromNotation(change.getNewValue());
        //lifelineCompartment.removeChild(lifelineToRemove);
        //storeNotationResource();
        //interaction.getLifeline(change.getNewValue().getName()).destroy();
        //storeModelResource();
    }

    @Override
    public void finalizeInterpretation() throws InterpreterException {
        storeModelResource();
        storeNotationResource();
    }

    private void storeNotationResource() throws InterpreterException {
        try {
            this.notationManager.getResource().save(null);
        } catch (IOException e) {
            throw new InterpreterException("Unable to update notation resource", e);
        }
    }

    private void storeModelResource() throws InterpreterException {
        try {
            this.modelManager.getResource().save(null);
        } catch (IOException e) {
            throw new InterpreterException("Unable to update model resource", e);
        }
    }

}
