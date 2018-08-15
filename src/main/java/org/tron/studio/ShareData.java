package org.tron.studio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.overlay.discover.node.NodeManager;
import org.tron.core.Wallet;
import org.tron.core.WalletSolidity;
import org.tron.core.db.Manager;
import org.tron.studio.solc.CompilationResult;

public class ShareData {

    public static Wallet wallet;

    private static Map<String, CompilationResult> compilationResultHashMap = new HashMap<>();

    //当前正在编辑的合约
    public static SimpleObjectProperty<String> currentContractName = new SimpleObjectProperty<>();
    //新建的合约
    public static SimpleObjectProperty<String> newContractName = new SimpleObjectProperty<>();
    //所有被打开的合约列表
    public static SimpleListProperty<String> activeContractName = new SimpleListProperty<>(
        FXCollections.observableArrayList());
    //所有的合约列表
    public static SimpleListProperty<String> allContractName = new SimpleListProperty<>(
        FXCollections.observableArrayList());

    private ShareData() {
    }

    public static CompilationResult getCompilationResult(String contractName) {
        return compilationResultHashMap.get(contractName);
    }

    public static void setCompilationResult(String contractName,
        CompilationResult compilationResult) {
        compilationResultHashMap.put(contractName, compilationResult);
    }

}