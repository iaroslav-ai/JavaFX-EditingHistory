/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package propertyhistorydb;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

/**
 *
 * @author Iaroslav
 */
public class EditingHistory {

    // these are used for demonstration
    public SimpleIntegerProperty historyLength = new SimpleIntegerProperty();
    public SimpleIntegerProperty historyPosition = new SimpleIntegerProperty();

    ObservableLinkedList<AbstractChange> undoList = new ObservableLinkedList<>();
    ObservableLinkedList<AbstractChange> redoList = new ObservableLinkedList<>();

    boolean historyIsTraversed = false;

    public EditingHistory() {
        undoList.AddListener((a1, a2, a3) -> {
            historyLength.set(undoList.size() + redoList.size());
        });
        redoList.AddListener((a1, a2, a3) -> {
            historyLength.set(undoList.size() + redoList.size());
        });

        historyPosition.addListener((oval, before, after) -> {
            
            if (historyIsTraversed) {
                return;
            }
            
            int diff = redoList.size() - historyPosition.get();

            for (int i = 0; i < Math.abs(diff); i++) {
                if (diff < 0) {
                    undo();
                }
                else{
                    redo();
                }
            }
        });
    }

    // util.LinkedList is not observable, making it so to observe changes to
    // undo / redo queue
    public class ObservableLinkedList<E> extends LinkedList<E> {

        ChangeListener listener = null;

        public void InvokeChangeListener() {
            listener.changed(null, null, null);
        }

        public void AddListener(ChangeListener lst) {
            listener = lst;

        }

        public void Add(E e) {
            super.add(e);
            InvokeChangeListener();
        }

        @Override
        public void addFirst(E e) {
            super.addFirst(e);
            InvokeChangeListener();
        }

        @Override
        public void addLast(E e) {
            super.addLast(e);
            InvokeChangeListener();

        }

        @Override
        public E peek() {
            E value = super.peek();
            InvokeChangeListener();
            return value;
        }

        @Override
        public void clear() {
            super.clear();
            InvokeChangeListener();
        }

    }

    public interface AbstractChange {

        // for some object, this method reverts its state to previous one
        void Revert();

        // returns instance of change that is opposite to current one, e.g.
        // inverse of adding an element is deleting same element from array
        // This method is called before the undo / redo is performed to be 
        // able to return to current state of an object
        AbstractChange Inverse();

    }

    // Change class for properties
    public class PropertyEditingChange implements AbstractChange {

        WritableValue propertyEdited;
        Object value;

        public PropertyEditingChange(WritableValue source) {
            propertyEdited = source;
        }

        @Override
        public void Revert() {
            propertyEdited.setValue(value);
        }

        @Override
        public AbstractChange Inverse() {

            PropertyEditingChange result = new PropertyEditingChange(this.propertyEdited);

            result.propertyEdited = this.propertyEdited;
            result.value = result.propertyEdited.getValue();

            return result;
        }

    }

    public class ArrayEditingChange implements AbstractChange {

        ObservableList arrayEdited;
        List changeData; // added / deleted elements
        int position; // position where change occured        
        int typeOfChange; // 1: add, 0: change, -1: remove

        @Override
        public void Revert() {

            
            if (typeOfChange == 1) { // remove added elements
                arrayEdited.remove(position, position + changeData.size());
            }

            if (typeOfChange == -1) { // put removed elements back
                if (position < arrayEdited.size()) {
                    arrayEdited.addAll(position, changeData);
                } else {
                    arrayEdited.addAll(changeData);
                }
            }

            if (typeOfChange == 0) { // restore previous value of array element
                arrayEdited.set(position, changeData.get(0));
            }

        }

        @Override
        public ArrayEditingChange Inverse() {

            ArrayEditingChange result = new ArrayEditingChange();
            ArrayEditingChange thisChange = this;

            result.typeOfChange = -thisChange.typeOfChange;

            result.position = thisChange.position;
            result.arrayEdited = thisChange.arrayEdited;

            result.changeData = result.typeOfChange == 0
                    ? new ArrayList() {
                        {
                            add(thisChange.arrayEdited.get(thisChange.position));
                        }
                    } : thisChange.changeData;

            return result;

        }

    }

    public void AddObservableList(ObservableList observableList) {

        // attach listener to array to record all changes
        // WARNING: PERMUTATION CHANGES ARE NOT SUPPORTED
        observableList.addListener((Change c) -> {
            
            // if the state of array is being restored by undo / redo commands,
            // then do nothing
            if (historyIsTraversed) {
                return;
            }

            // record all changes to array
            while (c.next()) {

                ArrayEditingChange edit = new ArrayEditingChange();
                edit.arrayEdited = observableList;

                if (c.wasReplaced()) {
                    edit.position = c.getFrom();
                    edit.changeData = new ArrayList(c.getRemoved());
                    edit.typeOfChange = 0;
                } else {
                    if (c.wasAdded()) {
                        edit.position = c.getFrom();
                        edit.changeData = new ArrayList(c.getAddedSubList());
                        edit.typeOfChange = 1;
                    } else {
                        edit.position = c.getFrom();
                        edit.changeData = new ArrayList(c.getRemoved());
                        edit.typeOfChange = -1;
                    }

                    if (c.wasAdded() && c.wasRemoved()) {
                        System.out.println("WARNING: PERMUTATION CHANGES ARE NOT SUPPORTED");
                    }
                }

                undoList.offerFirst(edit);
                
                // By default redo list is cleared as soon as element is added
                // to undoList
                redoList.clear();
            }
        });

    }

    public void AddProperty(Property prop) {

        // attach listener to property to record all changes
        prop.addListener((data, before, after) -> {
            if (!historyIsTraversed) {

                PropertyEditingChange step = new PropertyEditingChange((WritableValue) data);
                step.value = before;

                undoList.offerFirst(step);
                // By default redo list is cleared as soon as element is added
                // to undoList
                redoList.clear();

            }
        });

    }

    // universal function to perform undo or redo
    public void ChangeState(LinkedList<AbstractChange> fromList, LinkedList<AbstractChange> toList) {
        
        if (fromList.isEmpty()) {
            return;
        }

        historyIsTraversed = true;
        AbstractChange change = fromList.poll();

        // Create inverse of a change before reverting it ... 
        toList.offerFirst(change.Inverse());
        change.Revert();
        
        historyPosition.set(redoList.size());
        historyIsTraversed = false;
    }

    public void undo() {
        // go back in history
        ChangeState(undoList, redoList);
    }

    public void redo() {
        // go forward in history
        ChangeState(redoList, undoList);
    }

}
