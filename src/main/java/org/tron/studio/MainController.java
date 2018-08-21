package org.tron.studio;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXRippler;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.tron.studio.ui.SolidityHighlight;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class MainController {
    public CodeArea codeArea;
    public TabPane codeAreaTabPane;
    public JFXListView<Object> transactionHistoryListView;

    public Tab defaultTab;
    public TabPane rightContentTabPane;

    @PostConstruct
    public void initialize() throws IOException {
        StringBuilder builder = new StringBuilder();
        try {
            Files.lines(Paths.get(getClass().getResource("/template/Ballot.sol").getPath())).forEach(line -> {
                builder.append(line).append(System.getProperty("line.separator"));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        defaultTab.setOnCloseRequest(new EventHandler<Event>() {
            @Override
            public void handle(Event event) {
                int tabs_size = codeAreaTabPane.getTabs().size();
                System.out.println(tabs_size);
                if (tabs_size == 1) {
                    // Create blank file when closing last file
                    ShareData.newContractFileName.set("/template/Ballot.sol");
                }
            }
        });

        new SolidityHighlight(codeArea).highlight();
        codeArea.replaceText(0, 0, builder.toString());
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        SingleSelectionModel<Tab> selectionModel = codeAreaTabPane.getSelectionModel();

        ShareData.selectFile.addListener((observable, oldValue, newValue) -> {
            selectionModel.select(ShareData.currentFileIndex);
        });

        ShareData.addTransactionAction.addListener((observable, oldValue, newValue) -> {
            String currentContractName = ShareData.currentContractName.get();
            String transactionHeadMsg = String.format("creation of %s pending...", currentContractName);
            transactionHistoryListView.getItems().add(new Label(transactionHeadMsg));

            String[] labels = {"labels"};

            JFXListView<Object> subList = createList(labels);

            transactionHistoryListView.getItems().add(subList);

            /**
             int transationResCnt = ShareData.currentTransactionExtention.getConstantResultCount();

             if (transationResCnt == 0)
             {
             int signatureCount = ShareData.currentTransactionExtention.getTransaction().getSignatureCount();
             } **/
        });

        ShareData.newContractFileName.addListener((observable, oldValue, newValue) -> {
            try {
                Tab codeTab = FXMLLoader.load(getClass().getResource("ui/code_panel.fxml"));
                codeTab.setText(newValue);
                codeTab.setClosable(true);

                codeTab.setOnCloseRequest(new EventHandler<Event>() {
                    @Override
                    public void handle(Event event) {
                        int tabs_size = codeAreaTabPane.getTabs().size();
                        System.out.println(tabs_size);
                        if (tabs_size == 1) {
                            // Create blank file when closing last file
                            ShareData.newContractFileName.set("/template/Ballot.sol");
                        }
                    }
                });

                codeAreaTabPane.getTabs().add(codeTab);
                StringBuilder templateBuilder = new StringBuilder();
                templateBuilder.append("pragma solidity ^0.4.0;").append("\n");
                templateBuilder.append("contract ").append(newValue).append(" {").append("\n");
                templateBuilder.append("}").append("\n");
                CodeArea codeArea = (CodeArea) codeTab.getContent();

                new SolidityHighlight(codeArea).highlight();
                codeArea.replaceText(0, 0, templateBuilder.toString());
                codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

                codeAreaTabPane.getSelectionModel().select(codeTab);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        });
    }

    private TransactionTableView<TransactionDetail> createDetailTable() {
        TransactionTableView<TransactionDetail> table = new TransactionTableView<TransactionDetail>();

        TableColumn<TransactionDetail, String> fieldNameCol = new TableColumn("Field Name");
        fieldNameCol.setMinWidth(50);
        fieldNameCol.setCellValueFactory(
                new PropertyValueFactory<TransactionDetail, String>("fieldName"));

        TableColumn<TransactionDetail, String> valueCol = new TableColumn("Value");
        valueCol.setMinWidth(200);
        valueCol.setCellValueFactory(
                new PropertyValueFactory<TransactionDetail, String>("value"));

        // dummy data
        final ObservableList<TransactionDetail> data =
                FXCollections.observableArrayList(
                        new TransactionDetail("status", "0x1 Transaction mined and execution succeed"),
                        new TransactionDetail("transaction hash", "0xdbc3c61781c3f61d09339b98a553a3d1ddf9a00e5888574c8cba1c7b00a81a62"),
                        new TransactionDetail("contract address", "0x692a70d2e424a56d2c6c27aa97d1a86395877b3a")
                );

        table.setItems(data);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(fieldNameCol, valueCol);

        return table;
    }

    private JFXListView<Object> createList(String[] labels) {
        JFXListView<Object> subList = new JFXListView<>();

        // Create table to show details of transaction


        //subList.getItems().add(new Label("details"));
        subList.getItems().add(createDetailTable());

        Node node = new HBox();

        JFXRippler ripper = new JFXRippler();
        ripper.setStyle(":cons-rippler1");
        ripper.setPosition(JFXRippler.RipplerPos.FRONT);

        StackPane pane = new StackPane();
        pane.setStyle(":-fx-padding: 2;");

        MaterialDesignIconView copyIcon = new MaterialDesignIconView();
        copyIcon.setGlyphName("BUG");
        copyIcon.setStyleClass("icon");
        pane.getChildren().add(copyIcon);

        ripper.getChildren().add(pane);

        ((HBox) node).getChildren().add(ripper);

        String debugInfo = "[vm] from: xxxx to:Ballot.xxxx \n value:0 data: xxxxx. logs:0 hash:xxxx";
        Label debugInfoLabel = new Label(debugInfo);
        ((HBox) node).getChildren().add(debugInfoLabel);

        Button debugBtn = new Button("Debug");
        ((HBox) node).getChildren().add(debugBtn);
        SingleSelectionModel<Tab> selectionModel = rightContentTabPane.getSelectionModel();

        debugBtn.setOnAction(event -> {
            selectionModel.select(2);
            ShareData.debugRun.set("run");
        });

        subList.setGroupnode(node);

        return subList;
    }

    private String debugContract() {
        String address = ShareData.wallet.getClass().getName();
        String msg = String.format("from: %s", address);
        return msg;
    }

    public static class TransactionDetail {
        private final SimpleStringProperty fieldName;
        private final SimpleStringProperty value;

        private TransactionDetail(String fieldName, String value) {
            this.fieldName = new SimpleStringProperty(fieldName);
            this.value = new SimpleStringProperty(value);
        }

        public String getFieldName() {
            return fieldName.get();
        }

        public void setFieldName(String fieldName) {
            this.fieldName.set(fieldName);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }
    }

    class TransactionTableView<T> extends TableView<T> {
        @Override
        public void resize(double width, double height) {
            super.resize(width, height);
            Pane header = (Pane) lookup("TableHeaderRow");
            header.setMinHeight(0);
            header.setPrefHeight(0);
            header.setMaxHeight(0);
            header.setVisible(false);
        }
    }

}
