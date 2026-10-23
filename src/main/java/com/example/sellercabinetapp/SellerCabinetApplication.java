package com.example.sellercabinetapp;

import com.example.sellercabinetapp.client.ApiClient;
import com.example.sellercabinetapp.client.SellerApi;
import com.example.sellercabinetapp.dto.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;

import java.util.List;
import java.util.Optional;

public class SellerCabinetApplication extends Application {

    private SellerApi sellerApi;
    private boolean isAdmin = false;
    private Long currentSellerId = null;

    private final ListView<UserDto> sellerListView = new ListView<>();
    private final TableView<ProductDto> productTableView = new TableView<>();

    @Override
    public void start(Stage primaryStage) {
        ApiClient apiClient = new ApiClient("http://localhost:8085/");
        this.sellerApi = new SellerApi(apiClient);

        if (!showLoginDialog(apiClient)) {
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


    private JsonNode decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT");
            }
            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payloadJson = new String(decodedBytes);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(payloadJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT", e);
        }
    }

    private boolean showLoginDialog(ApiClient apiClient) {
        Dialog<LoginRequestDto> dialog = new Dialog<>();
        dialog.setTitle("Авторизация");
        dialog.setHeaderText("Вход в систему Seller Cabinet");

        ButtonType loginButtonType = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(emailField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == loginButtonType) {
                return new LoginRequestDto(emailField.getText().trim(), passwordField.getText().trim());
            }
            return null;
        });

        Optional<LoginRequestDto> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                TokenPairDto tokens = sellerApi.login(result.get().email(), result.get().password());
                apiClient.setJwtToken(tokens.accessToken());

                JsonNode payload = decodeJwtPayload(tokens.accessToken());

                currentSellerId = payload.get("sub").asLong();

                JsonNode rolesNode = payload.get("roles");
                if (rolesNode != null && rolesNode.isArray()) {
                    for (JsonNode role : rolesNode) {
                        if ("ADMIN".equals(role.asText())) {
                            isAdmin = true;
                            break;
                        }
                    }
                }

                return true;
            } catch (Exception e) {
                showAlert("Ошибка входа", e.getMessage());
                return showLoginDialog(apiClient);
            }
        }
        return false;
    }

    private Parent createAdminLayout() {
        VBox sellerBox = new VBox(10);
        sellerBox.setPadding(new Insets(10));
        Label lblSellers = new Label("Все пользователи");
        lblSellers.setStyle("-fx-font-weight: bold;");

        sellerListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(UserDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.firstName() + " " + item.lastName() + " (ID: " + item.userId() + ")");
            }
        });

        sellerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadProductsForSeller(newVal.userId());
            } else {
                productTableView.getItems().clear();
            }
        });

        Button btnDeleteSeller = new Button("Удалить выбранного пользователя");
        btnDeleteSeller.setOnAction(e -> {
            UserDto selected = sellerListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteUser(selected.userId());
            } else {
                showAlert("Ошибка", "Выберите пользователя для удаления");
            }
        });

        sellerBox.getChildren().addAll(lblSellers, sellerListView, btnDeleteSeller);
        VBox.setVgrow(sellerListView, Priority.ALWAYS);

        VBox productBox = createProductTableBase("Товары выбранного пользователя");
        HBox productButtons = new HBox(5);
        Button btnDeleteProduct = new Button("Удалить товар пользователя");

        btnDeleteProduct.setOnAction(e -> {
            UserDto selectedSeller = sellerListView.getSelectionModel().getSelectedItem();
            ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
            if (selectedSeller != null && selectedProduct != null) {
                deleteProduct(selectedProduct.productId(), () -> loadProductsForSeller(selectedSeller.userId()));
            } else {
                showAlert("Ошибка", "Выберите пользователя и его товар для удаления");
            }
        });

        productButtons.getChildren().add(btnDeleteProduct);
        productBox.getChildren().add(productButtons);

        SplitPane splitPane = new SplitPane(sellerBox, productBox);
        splitPane.setDividerPositions(0.4);

        loadAllUsers();
        return splitPane;
    }

    private Parent createSellerLayout() {
        VBox productBox = createProductTableBase("Мои товары");

        HBox buttons = new HBox(5);
        Button btnAdd = new Button("Добавить товар");
        Button btnEdit = new Button("Редактировать");
        Button btnDelete = new Button("Удалить");

        btnAdd.setOnAction(e -> showProductDialog(null));

        btnEdit.setOnAction(e -> {
            ProductDto selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showProductDialog(selected);
            } else {
                showAlert("Ошибка", "Выберите товар для редактирования");
            }
        });

        btnDelete.setOnAction(e -> {
            ProductDto selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteProduct(selected.productId(), this::loadMyProducts);
            } else {
                showAlert("Ошибка", "Выберите товар для удаления");
            }
        });

        buttons.getChildren().addAll(btnAdd, btnEdit, btnDelete);
        productBox.getChildren().add(buttons);

        loadMyProducts();
        return productBox;
    }

    private VBox createProductTableBase(String headerText) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        Label label = new Label(headerText);
        label.setStyle("-fx-font-weight: bold;");

        TableColumn<ProductDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().productId()));

        TableColumn<ProductDto, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().name()));

        TableColumn<ProductDto, Double> colPrice = new TableColumn<>("Цена");
        colPrice.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().price()));

        TableColumn<ProductDto, Integer> colCount = new TableColumn<>("В наличии");
        colCount.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().count()));

        TableColumn<ProductDto, Integer> colSales = new TableColumn<>("Продано");
        colSales.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().countOfSales()));

        TableColumn<ProductDto, Double> colEstimation = new TableColumn<>("Средняя оценка");
        colEstimation.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().averageEstimation()));

        productTableView.getColumns().clear();
        productTableView.getColumns().addAll(colId, colName, colPrice, colCount, colSales, colEstimation);
        productTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(productTableView, Priority.ALWAYS);
        box.getChildren().addAll(label, productTableView);
        return box;
    }

    // --- Загрузки ---

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                List<UserDto> users = sellerApi.getAllUsers();
                Platform.runLater(() -> sellerListView.getItems().setAll(users));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка сети", e.getMessage()));
            }
        }).start();
    }

    private void loadProductsForSeller(Long sellerId) {
        new Thread(() -> {
            try {
                List<ProductDto> products = sellerApi.getProductsBySeller(sellerId);
                Platform.runLater(() -> productTableView.getItems().setAll(products));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    productTableView.getItems().clear();
                    showAlert("Информация", "Не удалось загрузить товары: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadMyProducts() {
        new Thread(() -> {
            try {
                List<ProductDto> products = sellerApi.getAllProducts();
                Platform.runLater(() -> productTableView.getItems().setAll(products));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    private void deleteUser(Long userId) {
        new Thread(() -> {
            try {
                sellerApi.deleteUser(userId);
                Platform.runLater(this::loadAllUsers);
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    private void deleteProduct(Long productId, Runnable onSuccess) {
        new Thread(() -> {
            try {
                sellerApi.deleteProduct(productId);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    // --- Диалог создания/редактирования товара ---

    private void showProductDialog(ProductDto existing) {
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

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                String name = nameField.getText().trim();
                double price = parseDouble(priceField.getText());
                int count = parseInt(countField.getText());
                int sales = parseInt(salesField.getText());
                double estimation = parseDouble(estimationField.getText());
                return new ProductRequest(name, price, count, sales, estimation);
            }
            return null;
        });

        Optional<ProductRequest> result = dialog.showAndWait();
        result.ifPresent(req -> {
            new Thread(() -> {
                try {
                    if (existing == null) {
                        sellerApi.createProduct(req);
                    } else {
                        sellerApi.updateProduct(existing.productId(), req);
                    }
                    Platform.runLater(this::loadMyProducts);
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Ошибка сохранения", e.getMessage()));
                }
            }).start();
        });
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
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