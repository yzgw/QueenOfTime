package queen;

import com.sun.javafx.application.PlatformImpl;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main extends Application {

    private static final int INTERVAL_SEC = 10;

    private static final int DAY_JUMP_HOUR = 8;

    private static final String FILE_PATH = "./log.txt";

    private static final String SYSTEM_URL = "https://s2.kingtime.jp/independent/recorder/personal/";

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static DateTimeFormatter updatedFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private LocalDate selectedDate = LocalDate.now();

    private Point prevPosition = new Point(0, 0);

    private TextArea resultText;

    private Label updateLabel;

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(16));

        HBox header = new HBox();

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(this.selectedDate);
        header.getChildren().add(datePicker);

        Button openKingButton = new Button();
        openKingButton.setText("打刻");
        openKingButton.setOnMouseClicked(event -> {
            this.openKing();
        });
        header.getChildren().add(openKingButton);

        HBox.setMargin(openKingButton, new Insets(0, 0, 0, 16));
        wrapper.getChildren().add(header);

        this.resultText = new TextArea();
        this.resultText.setMaxWidth(300);
        this.resultText.setMaxHeight(100);
        this.resultText.setText("ここに結果が表示されます");
        VBox.setMargin(this.resultText, new Insets(16, 0, 0, 0));
        wrapper.getChildren().add(this.resultText);

        this.updateLabel = new Label();
        VBox.setMargin(this.updateLabel, new Insets(16, 0, 0, 0));
        wrapper.getChildren().add(this.updateLabel);

        datePicker.setOnAction(event -> {
            this.update(this.calc(datePicker.getValue()));
        });

        this.update(this.calc(LocalDate.now()));

        Scene rootScene = new Scene(wrapper);
        primaryStage.setTitle("Queen of Time");
        primaryStage.setResizable(false);
        primaryStage.setScene(rootScene);
        primaryStage.show();
        Timeline timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(INTERVAL_SEC), (ActionEvent event) -> {
            Point position = MouseInfo.getPointerInfo().getLocation();
            if (!this.prevPosition.equals(position)) {
                this.write();
            }
            Day day = this.calc(datePicker.getValue());
            this.update(day);

            boolean dayChanged = day != null
                    && ZonedDateTime.now().getDayOfMonth() != day.getEnded().getDayOfMonth()
                    && Math.abs(Duration.between(ZonedDateTime.now(), day.getEnded()).toHours()) > DAY_JUMP_HOUR;

            if (dayChanged) {
                // 日付が変わっていたら自動で今日に移動
                datePicker.setValue(LocalDate.now());
                this.openKing();
            }

            this.prevPosition = position;
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

    }

    private void update(Day day) {
        if (day != null) {
            String result = "";
            result += "開始：" + formatter.format(day.getStarted()) + "\n";
            result += "終了：" + formatter.format(day.getEnded()) + "\n";

            for (Interval rest : day.getRests()) {
                result += "休憩：" + formatter.format(rest.getStarted()) + " 〜 " + formatter.format(rest.getEnded()) + "(" + rest.getDuration().toMinutes() + "分)" + "\n";
            }
            this.resultText.setText(result);
        } else {
            this.resultText.setText("この日のデータはないようです");
        }
        this.updateLabel.setText("最終データ更新：" + updatedFormatter.format(ZonedDateTime.now()));
    }

    private Day calc(LocalDate date) {
        ArrayList<ZonedDateTime> today = new ArrayList<ZonedDateTime>();

        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                long timestamp = Long.parseLong(line);
                ZonedDateTime d = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

                // 相当する日
                if (date.getYear() == d.getYear() && date.getMonth() == d.getMonth() && date.getDayOfMonth() == d.getDayOfMonth()) {
                    today.add(d);
                }
            }
        } catch (IOException error) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ファイルの読み込み失敗");
            alert.setContentText("ファイルの読み込みに失敗しました。開発者にご連絡ください");
            alert.showAndWait();
            Platform.exit();
        }

        ArrayList<Interval> rests = new ArrayList<Interval>();

        if (today.size() > 0) {

            for (int i = 0; i < today.size() - 1; i++) {
                ZonedDateTime a = today.get(i);
                ZonedDateTime b = today.get(i + 1);
                Interval interval = new Interval(a, b);
                if (interval.isRest()) {
                    rests.add(interval);
                }
            }
            return new Day(today.get(0), today.get(today.size() - 1), rests);
        } else {
            return null;
        }
    }

    private void write() {
        try {
            File file = new File(FILE_PATH);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(String.valueOf(System.currentTimeMillis()));
            writer.newLine();
            writer.close();
        } catch (IOException error) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("ファイルの書き込み失敗");
            alert.setContentText("ファイルの書き込みに失敗しました。開発者にご連絡ください。" + error);
            alert.showAndWait();
            Platform.exit();
        }
    }

    private void openKing() {
        try {
            Desktop.getDesktop().browse(new URI(SYSTEM_URL));
        } catch (IOException | URISyntaxException error) {
            error.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PlatformImpl.setTaskbarApplication(false);
        launch(args);
    }
}
