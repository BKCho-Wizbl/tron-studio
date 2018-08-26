package org.tron.studio.ui;

import com.jfoenix.controls.*;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.protos.Protocol;
import org.tron.studio.ShareData;
import org.tron.studio.TransactionHistoryItem;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

public class TransactionHistoryController {

    public JFXListView<Object> transactionHistoryListView;


    @PostConstruct
    public void initialize() throws IOException {
        ShareData.addTransactionAction.addListener((observable, oldValue, newValue) -> {
            String currentContractName = ShareData.currentContractName.get();
            String transactionHeadMsg = String.format("creation of %s pending...", currentContractName);
            transactionHistoryListView.getItems().add(new Label(transactionHeadMsg));

            JFXListView<Object> subList = createSubList(ShareData.transactionHistory.get(newValue));
            transactionHistoryListView.getItems().add(subList);

            /**
             int transationResCnt = ShareData.currentTransactionExtention.getConstantResultCount();

             if (transationResCnt == 0)
             {
             int signatureCount = ShareData.currentTransactionExtention.getTransaction().getSignatureCount();
             } **/
        });
    }


    private JFXTreeTableView<TransactionDetail> createDetailTable() {

        JFXTreeTableView<TransactionDetail> detailTable = new JFXTreeTableView<>();
        JFXTreeTableColumn<TransactionDetail, String> keyCol = new JFXTreeTableColumn<>("Item");
        JFXTreeTableColumn<TransactionDetail, String> valueCol = new JFXTreeTableColumn<>("Value");
        detailTable.getColumns().add(keyCol);
        detailTable.getColumns().add(valueCol);

        setupCellValueFactory(keyCol, TransactionDetail::keyProperty);
        setupCellValueFactory(valueCol, TransactionDetail::valueProperty);

        GrpcAPI.TransactionExtention lastTransactionExtention = ShareData.wallet.getLastTransactionExtention();
        Protocol.Transaction lastTransaction = ShareData.wallet.getLastTransaction();
        String transactionId;
        ObservableList<TransactionDetail> detailTableData = null;
        if (lastTransactionExtention.getConstantResultCount() > 0) {
            transactionId = Hex.toHexString(lastTransactionExtention.getTxid().toByteArray());

            StringBuilder builder = new StringBuilder();
            lastTransactionExtention.getConstantResultList().forEach(result -> {
                builder.append(result.toStringUtf8()).append("\n");
            });

            detailTableData = FXCollections.observableArrayList(
                    new TransactionDetail("transaction_id", transactionId),
                    new TransactionDetail("log", builder.toString())
            );
        } else {
            transactionId = Hex.toHexString(new TransactionCapsule(lastTransaction).getTransactionId().getBytes());

            Protocol.TransactionInfo transactionInfo = ShareData.wallet.getTransactionInfoById(transactionId).get();
            StringBuilder builder = new StringBuilder();

            detailTableData = FXCollections.observableArrayList(
                    new TransactionDetail("transaction_id", transactionId),
                    new TransactionDetail("fee", Long.toString(transactionInfo.getFee())),
                    new TransactionDetail("block_number", Long.toString(transactionInfo.getBlockNumber())),
                    new TransactionDetail("time_stamp", Long.toString(transactionInfo.getBlockTimeStamp())),
                    new TransactionDetail("result", transactionInfo.getResult().equals(Protocol.TransactionInfo.code.SUCESS)?"success":"fail"),
                    new TransactionDetail("result_message", ByteArray.toStr(transactionInfo.getResMessage().toByteArray())),
                    new TransactionDetail("contract_result", ByteArray.toHexString(transactionInfo.getContractResult(0).toByteArray())),
                    new TransactionDetail("contract_address", Wallet.encode58Check(transactionInfo.getContractAddress().toByteArray())),
                    new TransactionDetail("energy_usage", Long.toString(transactionInfo.getReceipt().getEnergyUsage())),
                    new TransactionDetail("energy_fee(sun)", Long.toString(transactionInfo.getReceipt().getEnergyFee())),
                    new TransactionDetail("origin_energy_usage", Long.toString(transactionInfo.getReceipt().getOriginEnergyUsage())),
                    new TransactionDetail("energy_usage_total", Long.toString(transactionInfo.getReceipt().getEnergyUsageTotal())),
                    new TransactionDetail("net_usage", Long.toString(transactionInfo.getReceipt().getNetUsage())),
                    new TransactionDetail("net_fee", Long.toString(transactionInfo.getReceipt().getNetFee())),
                    new TransactionDetail("log", builder.toString())
            );
        }


        detailTable.setRoot(new RecursiveTreeItem<>(detailTableData, RecursiveTreeObject::getChildren));
        detailTable.setShowRoot(false);

        return detailTable;
    }

    private JFXListView<Object> createSubList(TransactionHistoryItem transactionHistoryItem) {
        JFXListView<Object> subList = new JFXListView<>();

        if (transactionHistoryItem.getType() == TransactionHistoryItem.Type.ERROR) {
            subList.getItems().add(new Label(transactionHistoryItem.getErrorInfo()));
        } else {
            subList.getItems().add(createDetailTable());
        }

        HBox node = new HBox();

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

        node.getChildren().add(ripper);

        String acountAddr = ShareData.currentAccount;
        acountAddr = acountAddr.substring(0, 5) + "..." + acountAddr.substring(acountAddr.length() - 5);

        String currentContract = ShareData.currentContractName.get();
        String currentValue = ShareData.currentValue;

        String debugInfo = "[vm] from: %s to: %s.(consructor)\n value:%s data: xxxxx. logs:0 hash:xxxx";
        debugInfo = String.format(debugInfo, acountAddr, currentContract, currentValue);
        Label debugInfoLabel = new Label(debugInfo);
        ((HBox) node).getChildren().add(debugInfoLabel);

        Region region1 = new Region();
        node.getChildren().add(region1);
        HBox.setHgrow(region1, Priority.ALWAYS);
        JFXButton debugBtn = new JFXButton("Debug");
        debugBtn.getStyleClass().add("custom-jfx-button-raised-fix-width");
        node.getChildren().add(debugBtn);
        Region region2 = new Region();
        region2.setPrefWidth(30);
        node.getChildren().add(region2);

        debugBtn.setOnAction(event -> {
            ShareData.debugTransactionAction.set(UUID.randomUUID().toString());
        });

        subList.setGroupnode(node);

        return subList;
    }

    static final class TransactionDetail extends RecursiveTreeObject<TransactionDetail> {

        final StringProperty keyProperty;
        final StringProperty valueProperty;

        TransactionDetail(String key, String val) {
            this.keyProperty = new SimpleStringProperty(key);
            this.valueProperty = new SimpleStringProperty(val);
        }

        StringProperty keyProperty() {
            return keyProperty;
        }

        StringProperty valueProperty() {
            return valueProperty;
        }
    }

    private <T> void setupCellValueFactory(JFXTreeTableColumn<TransactionDetail, T> column, Function<TransactionDetail, ObservableValue<T>> mapper) {
        column.setCellValueFactory((TreeTableColumn.CellDataFeatures<TransactionDetail, T> param) -> {
            if (column.validateValue(param)) {
                return mapper.apply(param.getValue().getValue());
            } else {
                return column.getComputedValue(param);
            }
        });
    }

}
