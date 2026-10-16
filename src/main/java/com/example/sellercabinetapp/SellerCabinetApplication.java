package com.example.sellercabinetapp;

import com.example.sellercabinetapp.client.SellerApi;
import com.example.sellercabinetapp.client.ApiClient;
import com.example.sellercabinetapp.dto.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Parent;

import java.util.Optional;
import java.util.List;

public class SellerCabinetApplication extends Application {

    private SellerApi sellerApi;

    private boolean isAdmin = false;
    private Long currentSellerId = null;

    private ListView<SellerDto> sellerListView = new ListView<>();
    private TableView<ProductDto> productTableView = new TableView<>();

    /*
        Здесь юзаем аннотацию Override, чтобы при сборке IoC контейнера внедрился
        бин с типом данных Stage, найденный среди ApplicationContext
        посредством работы механизма Spring Reflection. Способ инъекции в
        DI - в конструкторе(самый удобный и стабильный среди трех способов)
     */

    @Override
    public void start(Stage primaryStage){
        ApiClient apiClient = new ApiClient("http://localhost:8080/");
        this.sellerApi = new SellerApi(apiClient);

        if (!showLoginDialog()){
            System.exit(0);
            return;
        }

        Parent rootLayout;
        String title;

        if (isAdmin) {
            title = "Панель Администратора";
            rootLayout = createAdminLayout();
        } else {
            title = "Кабинет Продавца (ID: " + currentSellerId + ")";
            rootLayout = createSellerLayout();
        }

        Scene scene = new Scene(rootLayout, 900, 600);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean showLoginDialog(){
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Авторизация");
        dialog.setHeaderText("Вход в систему Seller Cabinet");
        dialog.setContentText("Введите 'admin' или Ваш ID продавца (число):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            String input = result.get().trim();
            if (input.equalsIgnoreCase("admin")) {
                isAdmin = true;
                return true;
            } else {
                try {
                    currentSellerId = Long.parseLong(input);
                    isAdmin = false;
                    return true;
                } catch(NumberFormatException e){
                    showAlert("Ошибка", "Неверный формат. Введите 'admin' или числовой ID.");
                    return showLoginDialog();
                }
            }
        }
        return false;
    }

    private Parent createAdminLayout(){
        VBox sellerBox = new VBox(10);
        sellerBox.setPadding(new Insets(10));
        Label lblSellers = new Label("Все продавцы (пользователи)");
        lblSellers.setStyle("fx-font-weight: bold;");

        sellerListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(SellerDto item, boolean empty){
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.firstName() + " " + item.lastName() + " (ID: " + item.sellerId() + ")");
            }
        });

        sellerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null){
                loadProducts(newVal.sellerId());
            } else {
                productTableView.getItems().clear();
            }
        });

        Button btnDeleteSeller = new Button("Удалить выбранного продавца");
        btnDeleteSeller.setOnAction(e -> {
            SellerDto selected = sellerListView.getSelectionModel().getSelectedItem();
            if (selected != null){
                deleteSeller(selected.sellerId());
            } else {
                showAlert("Ошибка", "Выберите продавца для удаления");
            }
        });

        sellerBox.getChildren().addAll(lblSellers, sellerListView, btnDeleteSeller);
        VBox.setVgrow(sellerListView, Priority.ALWAYS);

        VBox productBox = createProductTableBase("Товары выбранного продавца");
        HBox productButtons = new HBox(5);
        Button btnDeleteProduct = new Button("Удалить товар пользователя");

        btnDeleteProduct.setOnAction(e -> {
            SellerDto selectedSeller = sellerListView.getSelectionModel().getSelectedItem();
            ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
            if (selectedSeller != null && selectedProduct != null){
                deleteProduct(selectedSeller.sellerId(), selectedProduct.productId());
            } else {
                showAlert("Ошибка", "Выберите продавца и его товар для удаления");
            }
        });

        productButtons.getChildren().add(btnDeleteProduct);
        productBox.getChildren().add(productButtons);

        SplitPane splitPane = new SplitPane(sellerBox, productBox);
        splitPane.setDividerPositions(0.4);

        loadAllSellers();
        return splitPane;
    }

    private Parent createSellerLayout(){
        VBox productBox = createProductTableBase("Мои товары");

        HBox buttons = new HBox(5);
        Button btnAdd = new Button("Добавить товар");
        Button btnEdit = new Button("Редактировать");
        Button btnDelete = new Button("Удалить");

        btnAdd.setOnAction(e -> showProductDialog(currentSellerId, null));

        btnEdit.setOnAction(e -> {
            ProductDto selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null){
                showProductDialog(currentSellerId, selected);
            } else {
                showAlert("Ошибка", "Выберите товар для редактирования");
            }
        });

        btnDelete.setOnAction(e -> {
            ProductDto selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null){
                deleteProduct(currentSellerId, selected.productId());
            } else {
                showAlert("Ошибка", "Выберите товар для удаления");
            }
        });

        buttons.getChildren().addAll(btnAdd, btnEdit, btnDelete);
        productBox.getChildren().add(buttons);

        loadProducts(currentSellerId);
        return productBox;
    }

    private VBox createProductTableBase(String headerText){
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        Label label = new Label(headerText);
        label.setStyle("-fx-font-weight: bold;");

        TableColumn<ProductDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().productId()));

        TableColumn<ProductDto, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyStringWrapper(cellData.getValue().name()));

        TableColumn<ProductDto, Double> colPrice = new TableColumn<>("Цена");
        colPrice.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().price()));

        TableColumn<ProductDto, Integer> colCount = new TableColumn<>("В наличии");
        colCount.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().count()));

        TableColumn<ProductDto, Integer> colSales = new TableColumn<>("Продано");
        colSales.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().countOfSales()));

        TableColumn<ProductDto, Double> colEstimation = new TableColumn<>("Средняя оценка");
        colEstimation.setCellValueFactory(cellData -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().averageEstimation()));

        productTableView.getColumns().clear();
        productTableView.getColumns().addAll(colId, colName, colPrice, colCount, colSales, colEstimation);
        productTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(productTableView, Priority.ALWAYS);
        box.getChildren().addAll(label, productTableView);
        return box;
    }

    private void loadAllSellers(){
        new Thread(() -> {
            try {
                List<SellerDto> sellers = sellerApi.getAllSellers();
                Platform.runLater(() -> sellerListView.getItems().setAll(sellers));
            } catch (Exception e){
                Platform.runLater(() -> showAlert("Ошибка сети", e.getMessage()));
            }
        }).start();
    }

    private void deleteSeller(Long sellerId){
        new Thread(() -> {
            try {
                sellerApi.deleteSeller(sellerId);
                Platform.runLater(this::loadAllSellers);
            } catch(Exception e){
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    private void loadProducts(Long sellerId) {
        new Thread(() -> {
            try {
                List<ProductDto> products = sellerApi.getAllProducts(sellerId);
                Platform.runLater(() -> productTableView.getItems().setAll(products));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    productTableView.getItems().clear();
                    showAlert("Информация", "Не удалось загрузить товары (возможно, продавец не найден)");
                });
            }
        }).start();
    }

    private void deleteProduct(Long sellerId, Long productId) {
        new Thread(() -> {
            try {
                sellerApi.deleteProduct(sellerId, productId);
                Platform.runLater(() -> loadProducts(sellerId));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    private void showProductDialog(Long sellerId, ProductDto existing){
        Dialog<ProductRequest> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Добавить товар" : "Редактировать товар");
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Название товара");

        TextField priceField = new TextField();
        priceField.setPromptText("Цена (например 123.99)");

        TextField countField = new TextField();
        countField.setPromptText("Количество в наличии");

        TextField salesField = new TextField();
        salesField.setPromptText("Сколько продано");

        TextField estimationField = new TextField();
        estimationField.setPromptText("Средняя оценка (от 1.00 до 5.00)");

        if (existing != null) {
            nameField.setText(existing.name());
            priceField.setText(String.valueOf(existing.price()));
            countField.setText(String.valueOf(existing.count()));
            salesField.setText(String.valueOf(existing.countOfSales()));
            estimationField.setText(String.valueOf(existing.averageEstimation()));
        }

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Цена:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("В наличии:"), 0, 2);
        grid.add(countField, 1, 2);
        grid.add(new Label("Продано:"), 0, 3);
        grid.add(salesField, 1, 3);
        grid.add(new Label("Средняя оценка:"), 0, 4);
        grid.add(estimationField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String name = nameField.getText().trim();

                double price = 0.0;
                try {
                    price = Double.parseDouble(priceField.getText().trim());
                } catch (NumberFormatException ignored) {}

                int count = 0;
                try {
                    count = Integer.parseInt(countField.getText().trim());
                } catch (NumberFormatException ignored) {}

                int sales = 0;
                try {
                    sales = Integer.parseInt(salesField.getText().trim());
                } catch (NumberFormatException ignored) {}

                double estimation = 0.0;
                try {
                    estimation = Double.parseDouble(estimationField.getText().trim());
                } catch (NumberFormatException ignored) {}

                return new ProductRequest(name, price, count, sales, estimation);
            }
            return null;
        });

        Optional<ProductRequest> result = dialog.showAndWait();
        result.ifPresent(req -> {
            new Thread(() -> {
                try {
                    if (existing == null){
                        sellerApi.createProduct(sellerId, req);
                    } else {
                        sellerApi.updateProduct(sellerId, existing.productId(), req);
                    }
                    Platform.runLater(() -> loadProducts(sellerId));
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Ошибка сохранения", e.getMessage()));
                }
            }).start();
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


