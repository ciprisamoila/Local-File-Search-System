package org.example.filebrowser.ui;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.example.filebrowser.crawler.Crawling;
import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.model.RankingStrategy;
import org.example.filebrowser.querylogic.IQuerier;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.ReportType;
import org.example.filebrowser.utils.exceptions.ConfigException;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SearchController {
    private static final int MAX_RESULTS = 100;
    private static final Pattern BOLD_SEGMENT_PATTERN = Pattern.compile("(?i)<b>(.*?)</b>", Pattern.DOTALL);
    private static final Pattern ANY_TAG_PATTERN = Pattern.compile("<[^>]+>");

    @FXML
    private TextField queryInput;

    @FXML
    private ChoiceBox<RankingStrategy> rankingStrategyChoice;

    @FXML
    private CheckBox increasingOrderCheckBox;

    @FXML
    private Button searchButton;

    @FXML
    private TextField crawlRootInput;

    @FXML
    private ChoiceBox<ReportType> crawlReportTypeChoice;

    @FXML
    private TextField crawlFileTypesInput;

    @FXML
    private TextField crawlMaxFileSizeInput;

    @FXML
    private Button crawlButton;

    @FXML
    private Label crawlStatusLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ListView<QueryFileModel> resultsList;

    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "query-worker");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService crawlExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread t = new Thread(runnable, "crawl-worker");
        t.setDaemon(true);
        return t;
    });

    private IQuerier querier;
    private Crawling crawler;
    private Future<?> runningSearch;
    private Future<?> runningCrawl;
    private int searchRequestId;
    private boolean crawlConfigLoaded;
    private CrawlConfig initialConfig;

    @FXML
    private void initialize() {
        rankingStrategyChoice.setItems(FXCollections.observableArrayList(RankingStrategy.values()));
        rankingStrategyChoice.setValue(RankingStrategy.RELEVANCE);
        increasingOrderCheckBox.setSelected(true);
        crawlReportTypeChoice.setItems(FXCollections.observableArrayList(ReportType.values()));

        searchButton.setDisable(true);
        rankingStrategyChoice.setDisable(true);
        increasingOrderCheckBox.setDisable(true);
        crawlButton.setDisable(true);
        resultsList.setPlaceholder(new Label("No results yet."));
        resultsList.setCellFactory(_ -> new QueryResultCell());
        queryInput.setOnAction(_ -> onSearchClicked());
        rankingStrategyChoice.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> executeSearch(false));
        increasingOrderCheckBox.selectedProperty().addListener((_, _, _) -> executeSearch(false));
        statusLabel.setText("Connecting to database...");

        try {
            loadCurrentCrawlConfig();
        } catch (ConfigException e) {
            crawlStatusLabel.setText(e.getMessage());
        }
    }

    public void setQuerier(IQuerier querier) {
        this.querier = querier;
        searchButton.setDisable(false);
        rankingStrategyChoice.setDisable(false);
        increasingOrderCheckBox.setDisable(false);
        queryInput.setDisable(false);
        statusLabel.setText("Ready. Insert a query and press Search.");
    }

    public void setCrawler(Crawling crawler) {
        this.crawler = crawler;
        updateCrawlButtonState();
    }

    public void runStartCrawl() {
        crawlButton.setDisable(true);

        try {
            crawler.crawl();
            crawlStatusLabel.setText("Crawl finished with the current configuration.");
            crawlButton.setDisable(false);
        } catch (IndexUpdaterException e) {
            crawlStatusLabel.setText("Crawl failed: " + e.getMessage());
            crawlButton.setDisable(false);
        }
    }

    public void setInitializationError(String message) {
        queryInput.setDisable(true);
        searchButton.setDisable(true);
        rankingStrategyChoice.setDisable(true);
        increasingOrderCheckBox.setDisable(true);
        statusLabel.setText(message);
    }

    @FXML
    private void onSearchClicked() {
        executeSearch(true);
    }

    @FXML
    private void onCrawlClicked() {
        if (runningCrawl != null && !runningCrawl.isDone()) {
            crawlStatusLabel.setText("A crawl is already running.");
            return;
        }

        CrawlConfig config;
        try {
            config = readConfigFromForm();
        } catch (IllegalArgumentException e) {
            crawlStatusLabel.setText(e.getMessage());
            return;
        }

        crawlButton.setDisable(true);
        crawlStatusLabel.setText("Saving configuration and running crawl...");

        Task<Void> crawlTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                CrawlConfig.writeConfigToFile(config);
                crawler.crawl();
                return null;
            }
        };

        crawlTask.setOnSucceeded(_ -> {
            crawlStatusLabel.setText("Crawl finished with the current configuration.");
            crawlButton.setDisable(false);
        });

        crawlTask.setOnFailed(_ -> {
            Throwable ex = crawlTask.getException();
            String errorMessage = ex == null ? "Unknown error." : ex.getMessage();
            crawlStatusLabel.setText("Crawl failed: " + errorMessage);
            crawlButton.setDisable(false);
        });

        runningCrawl = crawlExecutor.submit(crawlTask);
    }

    private void loadCurrentCrawlConfig() throws ConfigException {
        initialConfig = CrawlConfig.readConfigFromFileWithCreation();
        applyConfigToForm(initialConfig);
        crawlConfigLoaded = true;
        crawlStatusLabel.setText("Crawl configuration loaded.");
        updateCrawlButtonState();
    }

    private void updateCrawlButtonState() {
        crawlButton.setDisable(!crawlConfigLoaded || crawler == null);
    }

    private void applyConfigToForm(CrawlConfig config) {
        crawlRootInput.setText(config.root());
        crawlReportTypeChoice.setValue(config.reportType());
        crawlFileTypesInput.setText(String.join(", ", config.fileTypes()));
        crawlMaxFileSizeInput.setText(Long.toString(config.maxFileSize()));
    }

    private CrawlConfig readConfigFromForm() {
        String root = readRequiredText(crawlRootInput, "Root folder is required.");
        ReportType reportType = crawlReportTypeChoice.getValue();
        if (reportType == null) {
            throw new IllegalArgumentException("Select a report type.");
        }

        String fileTypesText = readRequiredText(crawlFileTypesInput, "At least one file type is required.");
        String[] fileTypes = parseFileTypes(fileTypesText);
        if (fileTypes.length == 0) {
            throw new IllegalArgumentException("Provide at least one file type.");
        }

        String maxSizeText = readRequiredText(crawlMaxFileSizeInput, "Max file size is required.");
        long maxFileSize;
        try {
            maxFileSize = Long.parseLong(maxSizeText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Max file size must be a number.");
        }

        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Max file size must be greater than zero.");
        }

        return new CrawlConfig(root, reportType, fileTypes, maxFileSize);
    }

    private static String readRequiredText(TextField field, String errorMessage) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private static String[] parseFileTypes(String fileTypesText) {
        return Stream.of(fileTypesText.split("[,\\n]"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toArray(String[]::new);
    }

    private void executeSearch(boolean fromButton) {
        if (querier == null) {
            statusLabel.setText("Query manager not available.");
            return;
        }

        String query = queryInput.getText() == null ? "" : queryInput.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText(fromButton ? "Please enter a search query." : "Type to search.");
            resultsList.getItems().clear();
            searchButton.setDisable(false);
            return;
        }

        if (runningSearch != null && !runningSearch.isDone()) {
            runningSearch.cancel(true);
        }

        int requestId = ++searchRequestId;
        searchButton.setDisable(true);
        statusLabel.setText("Searching...");

        Task<List<QueryFileModel>> task = new Task<>() {
            @Override
            protected List<QueryFileModel> call() throws QueryManagerException {
                RankingStrategy rankingStrategy = rankingStrategyChoice.getValue();
                if (rankingStrategy == null) {
                    rankingStrategy = RankingStrategy.RELEVANCE;
                }

                return querier.getNextFilesMatching(
                        new QuerySpecs(
                                MAX_RESULTS, 0,
                                rankingStrategy,
                                increasingOrderCheckBox.isSelected()
                        ), query);
            }
        };

        task.setOnSucceeded(_ -> {
            if (requestId != searchRequestId) {
                return;
            }

            List<QueryFileModel> results = task.getValue();
            resultsList.getItems().setAll(results);
            statusLabel.setText("Found " + results.size() + " result(s).");
            searchButton.setDisable(false);
        });

        task.setOnFailed(_ -> {
            if (requestId != searchRequestId) {
                return;
            }

            Throwable ex = task.getException();
            String errorMessage = ex == null ? "Unknown error." : ex.getMessage();
            statusLabel.setText("Search failed: " + errorMessage);
            searchButton.setDisable(false);
        });

        runningSearch = searchExecutor.submit(task);
    }

    public void shutdown() {
        searchExecutor.shutdownNow();
        crawlExecutor.shutdownNow();
    }

    private static final class QueryResultCell extends ListCell<QueryFileModel> {
        @Override
        protected void updateItem(QueryFileModel item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            Label nameLabel = new Label(item.fullName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label pathLabel = new Label(item.path());
            pathLabel.setStyle("-fx-text-fill: #425466;");

            Label creationTimeLabel = new Label("created: " + item.creation_time());
            creationTimeLabel.setStyle("-fx-text-fill: #425466;");

            Label lastModifiedTimeLabel = new Label("modified: " + item.last_modified_time());
            lastModifiedTimeLabel.setStyle("-fx-text-fill: #425466");

            Label lastAccessedTimeLabel = new Label("accessed: " + item.last_accessed_time());
            lastAccessedTimeLabel.setStyle("-fx-text-fill: #425466");

            Label sizeLabel = new Label("size: " + item.size());
            sizeLabel.setStyle("-fx-text-fill: #425466;");

            Label accessLabel = new Label(item.readAccess() ? "Readable" : "No read access");
            accessLabel.setStyle(item.readAccess() ? "-fx-text-fill: #1b5e20;" : "-fx-text-fill: #b71c1c;");

            TextFlow headlineFlow = buildHeadlineFlow(item.headline());
            headlineFlow.setStyle("-fx-fill: #1f2937;");

            VBox container = new VBox(4, nameLabel, pathLabel, creationTimeLabel,
                    lastModifiedTimeLabel, lastAccessedTimeLabel, sizeLabel, accessLabel, headlineFlow);
            container.setStyle("-fx-padding: 8 0 8 0;");

            setText(null);
            setGraphic(container);
        }

        private static TextFlow buildHeadlineFlow(String headline) {
            String safeHeadline = headline == null ? "" : headline;
            Matcher matcher = BOLD_SEGMENT_PATTERN.matcher(safeHeadline);

            TextFlow flow = new TextFlow();
            int start = 0;

            while (matcher.find()) {
                appendSegment(flow, safeHeadline.substring(start, matcher.start()), false);
                appendSegment(flow, matcher.group(1), true);
                start = matcher.end();
            }

            appendSegment(flow, safeHeadline.substring(start), false);
            return flow;
        }

        private static void appendSegment(TextFlow flow, String segment, boolean bold) {
            String sanitized = ANY_TAG_PATTERN.matcher(segment).replaceAll("");
            if (sanitized.isEmpty()) {
                return;
            }

            Text textNode = new Text(sanitized);
            if (bold) {
                textNode.setStyle("-fx-font-weight: bold;");
            }
            flow.getChildren().add(textNode);
        }
    }
}
