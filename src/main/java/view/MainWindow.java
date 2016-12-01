package view;

/*-
 * #%L
 * Zork Clone
 * %%
 * Copyright (C) 2016 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


/**
 * Sample Skeleton for 'BasicApplication_i18n.fxml' Controller Class
 */

import common.AppConfig;
import common.Common;
import common.UpdateChecker;
import common.UpdateInfo;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import logging.FOKLogger;
import parser.Parser;
import view.updateAvailableDialog.UpdateAvailableDialog;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;

public class MainWindow extends Application{

    private static FOKLogger log;
    private static boolean disableUpdateChecks;

    private static List<GameMessage> messages = new ArrayList<>();

    public static void main(String[] args){
        common.Common.setAppName("zork");
        log = new FOKLogger(MainWindow.class.getName());
        for (String arg : args) {
            if (arg.toLowerCase().matches("mockappversion=.*")) {
                // Set the mock version
                String version = arg.substring(arg.toLowerCase().indexOf('=') + 1);
                Common.setMockAppVersion(version);
            } else if (arg.toLowerCase().matches("mockbuildnumber=.*")) {
                // Set the mock build number
                String buildnumber = arg.substring(arg.toLowerCase().indexOf('=') + 1);
                Common.setMockBuildNumber(buildnumber);
            } else if (arg.toLowerCase().matches("disableupdatechecks")) {
                log.getLogger().info("Update checks are disabled as app was launched from launcher.");
                disableUpdateChecks = true;
            } else if (arg.toLowerCase().matches("mockpackaging=.*")) {
                // Set the mock packaging
                String packaging = arg.substring(arg.toLowerCase().indexOf('=') + 1);
                Common.setMockPackaging(packaging);
            } else if (arg.toLowerCase().matches("locale=.*")) {
                // set the gui language
                String guiLanguageCode = arg.substring(arg.toLowerCase().indexOf('=') + 1);
                log.getLogger().info("Setting language: " + guiLanguageCode);
                Locale.setDefault(new Locale(guiLanguageCode));
            }
        }

        launch(args);
    }

    public static ResourceBundle bundle;
    private Stage stage;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="commandLine"
    private TextField commandLine; // Value injected by FXMLLoader

    @FXML // fx:id="getAvailableCommandsButton"
    private Button getAvailableCommandsButton; // Value injected by FXMLLoader

    @FXML
    private WebView messageView;

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert commandLine != null : "fx:id=\"commandLine\" was not injected: check your FXML file 'BasicApplication_i18n.fxml'.";
        assert getAvailableCommandsButton != null : "fx:id=\"getAvailableCommandsButton\" was not injected: check your FXML file 'BasicApplication_i18n.fxml'.";
        messages.add(new GameMessage("ZORK I: The Great Underground Empire\nCopyright (c) 1981, 1982, 1983 Infocom, Inc. All rights reserved.\nZORK is a registered trademark of Infocom, Inc.\n Revison " + Common.getAppVersion() + "\n\nThis game is not yet functional. Give the team some time and come back in some time. See ya :)", true));
        updateCommandView();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        bundle = ResourceBundle.getBundle("view.strings");

        // appConfig = new Config();

        stage = primaryStage;
        try {
            Thread updateThread = new Thread(() -> {
                UpdateInfo update = UpdateChecker.isUpdateAvailable(AppConfig.getUpdateRepoBaseURL(),
                        AppConfig.groupID, AppConfig.artifactID, AppConfig.updateFileClassifier,
                        Common.getPackaging());
                if (update.showAlert) {
                    Platform.runLater(() -> new UpdateAvailableDialog(update));
                }
            });
            updateThread.setName("updateThread");
            updateThread.start();

            Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"), bundle);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("MainWindow.css").toExternalForm());

            primaryStage.setTitle(bundle.getString("windowTitle"));

            primaryStage.setMinWidth(scene.getRoot().minWidth(0) + 70);
            primaryStage.setMinHeight(scene.getRoot().minHeight(0) + 70);

            primaryStage.setScene(scene);

            // Set Icon
            primaryStage.getIcons().add(new Image(MainWindow.class.getResourceAsStream("icon.png")));

            primaryStage.show();
        } catch (Exception e) {
            log.getLogger().log(Level.SEVERE, "An error occurred", e);
        }
    }

    @FXML
    void commandLineOnKeyPressed(KeyEvent event) {
        if (event.getCode().equals(KeyCode.ENTER)){
            String playerMessage = this.commandLine.getText();
            messages.add(new GameMessage(playerMessage, false));
            messages.add(new GameMessage(Parser.parse(playerMessage), true));
            this.commandLine.setText("");
            updateCommandView();
        }
    }

    public void updateCommandView(){
        this.messageView.getEngine().loadContent(HTMLGenerator.generate(messages));
        System.out.println("===================================================================");
        System.out.println(HTMLGenerator.generate(messages));
    }
}
