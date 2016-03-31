/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package propertyhistorydb;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;

/**
 *
 * @author Iaroslav
 */
public class PropertyTesterFormController implements Initializable {

    private Label label;
    @FXML
    private ListView<ObservableList<StringProperty>> bigList;
    @FXML
    private ListView<StringProperty> smallList;
    @FXML
    private TextArea textEditor;
    @FXML
    private TextArea textEditor2;

    EditingHistory history = new EditingHistory();
    SimpleStringProperty prop = new SimpleStringProperty();
    List<ObservableList<StringProperty>> list = new ArrayList<>();
    ObservableList<ObservableList<StringProperty>> primalList = FXCollections.observableList(list);
    @FXML
    private Slider historySlider;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO

        ObservableList<StringProperty> olist = FXCollections.observableList(new ArrayList<>());
        primalList.add(olist);
        bigList.setItems(primalList);
        
        SetupSmallList();

        history.AddObservableList(primalList);
        history.AddObservableList(olist);

        historySlider.maxProperty().bind(history.historyLength);
        historySlider.valueProperty().bindBidirectional(history.historyPosition);

        AddDataToList(olist);
        
        bigList.getSelectionModel().select(0);

    }

    private void SetupSmallList() {
        
        smallList.itemsProperty().bind(bigList.getSelectionModel().selectedItemProperty());
        
        smallList.getSelectionModel().selectedItemProperty().addListener((ov, a1, a2) -> {
            if(a1 != null) textEditor.textProperty().unbindBidirectional(a1);
            if(a2 != null) textEditor.textProperty().bindBidirectional(a2);
        });
        
        smallList.setCellFactory((param) -> new PropertyListCell());
    }

    private void AddDataToList(ObservableList<StringProperty> olist) {
        int N = 13;
        int M1 = 7;
        int M2 = 5;
               
        for (int i = 0; i < N; i++) {

            prop = new SimpleStringProperty();
            prop.set(i + " ");
            olist.add(prop);
            history.AddProperty(prop);

        }
        
        for (int i = 0; i < N; i++) {
            int idx = (int) (Math.pow(i+1, M1)) % N;
            olist.get(idx).set(olist.get(idx).get()+" item");
        }
        
        for (int i = 0; i < N; i++) {
            int idx = (int) (Math.pow(i+1, M2)) % N;
            olist.get(idx).set(olist.get(idx).get()+" data");
        }
    }

    @FXML
    private void undoAction(ActionEvent event) {

        history.undo();
    }

    @FXML
    private void redoAction(ActionEvent event) {

        history.redo();
    }

    @FXML
    private void addToBigList(ActionEvent event) {

        List<StringProperty> blst = new ArrayList<>();
        ObservableList<StringProperty> olist = FXCollections.observableList(blst);
        history.AddObservableList(olist);
        primalList.add(olist);

    }

    @FXML
    private void deleteFromBigList(ActionEvent event) {

        primalList.remove(bigList.getSelectionModel().getSelectedItem());

    }

    @FXML
    private void addToSmallList(ActionEvent event) {

        ObservableList<StringProperty> olist
                = bigList.getSelectionModel().getSelectedItem();

        if (olist != null) {
            SimpleStringProperty tmp = new SimpleStringProperty();
            tmp.set("edit me!");
            olist.add(tmp);
            history.AddProperty(tmp);
        }
    }

    @FXML
    private void deleteFromSmallList(ActionEvent event) {

        ObservableList<StringProperty> olist
                = bigList.getSelectionModel().getSelectedItem();

        if (olist != null) {
            olist.removeAll(smallList.getSelectionModel().getSelectedItem());
        }

    }

    // class used for proper display of properties
    private static class PropertyListCell extends ListCell<StringProperty> {

        StringProperty previous = null;

        @Override
        public void updateItem(StringProperty item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {

                if (item != previous) {

                    Label lbl = null;

                    Node graphic = getGraphic();
                    if (graphic == null) {
                        lbl = new Label();
                        setGraphic(lbl);
                    } else {
                        graphic.setVisible(true);
                        lbl = (Label) graphic;
                    }

                    lbl.textProperty().unbind();
                    lbl.textProperty().bind(item);

                }
            } else {
                if (getGraphic() != null) {
                    getGraphic().setVisible(false);
                }
            }
            previous = item;
        }

    }

}
