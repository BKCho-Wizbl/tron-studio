package org.tron.studio.ui;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.core.exception.CancelException;
import org.tron.keystore.CipherException;
import org.tron.protos.Protocol.Transaction;
import org.tron.studio.ShareData;
import org.tron.studio.solc.CompilationResult;
import org.tron.studio.solc.CompilationResult.ContractMetadata;
import org.tron.studio.walletserver.WalletClient;

public class RightTabRunController implements Initializable {

  static final Logger logger = LoggerFactory.getLogger(RightTabRunController.class);
  public JFXComboBox<String> environmentComboBox;
  public JFXComboBox<String> contractComboBox;
  public JFXComboBox<String> accountComboBox;
  public JFXTextField feeLimitTextField;
  public JFXTextField valueTextField;
  public JFXComboBox feeUnitComboBox;
  public JFXComboBox valueUnitComboBox;
  public JFXTextField userPayRatio;
  public JFXListView deployedContractList;

  private static String DEFAULT_FEE_LIMIT = "1000000";
  private static String DEFAULT_VALUE = "0";
  private static String DEFAULT_RATIO = "100";

  public void initialize(URL location, ResourceBundle resources) {
    environmentComboBox.setItems(FXCollections.observableArrayList(
        "Local TVM",
        "Test Net",
        "Main Net"
    ));
    environmentComboBox.getSelectionModel().selectFirst();

    accountComboBox.setItems(FXCollections.observableArrayList(ShareData.testAccount.keySet()));
    accountComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
          String address = newValue;
          String privateKey = ShareData.testAccount.get(address);
          ShareData.wallet = new WalletClient(Hex.decode(privateKey));
        }
    );
    accountComboBox.getSelectionModel().selectFirst();

    feeUnitComboBox.setItems(FXCollections.observableArrayList(
        "TRX",
        "SUN"
    ));
    feeUnitComboBox.getSelectionModel().selectFirst();

    valueUnitComboBox.setItems(FXCollections.observableArrayList(
        "TRX",
        "SUN"
    ));
    valueUnitComboBox.getSelectionModel().selectFirst();

    feeLimitTextField.setText(DEFAULT_FEE_LIMIT);
    valueTextField.setText(DEFAULT_VALUE);
    userPayRatio.setText(DEFAULT_RATIO);

    reloadContract();
  }

  private void reloadContract() {
    ShareData.currentContractFileName.addListener((observable, oldValue, newValue) -> {
      List<String> contractNameList = new ArrayList<>();
      if (newValue != null) {
        CompilationResult compilationResult = ShareData.getCompilationResult(newValue);
        compilationResult.getContracts().forEach(contractResult -> {
          JSONObject metaData = JSON.parseObject(contractResult.metadata);
          JSONObject compilationTarget = metaData.getJSONObject("settings")
              .getJSONObject("compilationTarget");
          compilationTarget.forEach((sol, value) -> {
            contractNameList.add((String) value);
          });
        });
      }
      contractComboBox.setItems(FXCollections.observableArrayList(
          contractNameList
      ));
      contractComboBox.getSelectionModel().selectFirst();
    });
  }

  public void onClickDeploy(ActionEvent actionEvent) {
    CompilationResult result = ShareData
        .getCompilationResult(ShareData.currentContractFileName.get());
    if (result == null) {
      logger.error("No CompilationResult found");
      return;
    }
    String currentContractName = contractComboBox.valueProperty().get();

    ContractMetadata currentContract = result.getContract(currentContractName);

    boolean deployContractResult = false;
    try {
      deployContractResult = ShareData.wallet
          .deployContract(currentContractName, currentContract.abi, currentContract.bin,
              Long.parseLong(feeLimitTextField.getText()), Long.parseLong(valueTextField.getText()),
              Long.parseLong(userPayRatio.getText()), null);
    } catch (IOException | CipherException | CancelException e) {
      logger.error("Failed to deployContract{} {}", e.getMessage(), e);
      return;
    }

    if (!deployContractResult) {
      logger.error("Failed to deployContract, please check tron.log");
      return;
    }

    TransactionExtention transactionExtention = ShareData.wallet.getLastTransactionExtention();
    Transaction transaction = ShareData.wallet.getLastTransaction();
    if (!transactionExtention.getResult().getResult()) {
      logger.error("Unable to get last TransactionExtention, {}", transactionExtention.getResult().getMessage().toStringUtf8());
      return;
    }

    String transactionId = Hex.toHexString(transactionExtention.getTxid().toByteArray());
    deployedContractList.getItems().add(getContractRunPanel(transactionId, currentContract.abi));
  }

  private Pane getContractRunPanel(String transactionId, String abi) {
    VBox vbox = new VBox();
    Label transactionLabel = new Label("0x" + transactionId.substring(0, 3) + "..." + transactionId
        .substring(transactionId.length() - 3, transactionId.length()));
    vbox.getChildren().add(transactionLabel);
    List<String> abiJson = JSONArray.parseArray(abi, String.class);
    for (String entry : abiJson) {
      JSONObject entryJson = JSONObject.parseObject(entry);
      if (StringUtils.equalsIgnoreCase("function", entryJson.getString("type"))) {
        HBox item = new HBox();
        item.getChildren().add(new JFXButton(entryJson.getString("name")));
        item.getChildren().add(new JFXTextField());
        vbox.getChildren().add(item);
      }
    }
    return vbox;
  }


  public void onClickLoad(ActionEvent actionEvent) {
  }

  public void onClickClear(ActionEvent actionEvent) {
  }

  public void onClickAddAddress(MouseEvent mouseEvent) {
    logger.debug("onClickAddAddress");
  }

  public void onClickCopyAddress(MouseEvent mouseEvent) {
  }
}