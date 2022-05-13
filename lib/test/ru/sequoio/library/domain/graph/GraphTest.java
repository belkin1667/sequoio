package ru.sequoio.library.domain.graph;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sequoio.library.domain.migration.Migration;
import ru.sequoio.library.domain.migration.migration_paramters.MigrationParameter;
import ru.sequoio.library.domain.migration.migration_paramters.ParameterValue;
import ru.sequoio.library.domain.migration.migration_paramters.StringParameterValue;

class GraphTest {

    private int migrationCounter;

    @BeforeEach
    void setUp() {
        migrationCounter = 0;
    }

    @Test
    void testSimpleNaturalOrder() {
        var migrations = List.of(migration(), migration());
        var graph = new Graph<>(migrations);
        var orderedMigrations = graph.getOrderedNodes();
        validateOrder(orderedMigrations, order(1, 2));
    }

    @Test
    void testSimpleRunBefore() {
        var migrations = List.of(
                migration(),
                migration(Map.of(MigrationParameter.RUN_BEFORE, new StringParameterValue("title_1"))));
        var graph = new Graph<>(migrations);
        var orderedMigrations = graph.getOrderedNodes();
        validateOrder(orderedMigrations, order(2, 1));
    }

    @Test
    void testSimpleRunAfter() {
        var migrations = List.of(
                migration(Map.of(MigrationParameter.RUN_AFTER, new StringParameterValue("title_2"))),
                migration());
        var graph = new Graph<>(migrations);
        var orderedMigrations = graph.getOrderedNodes();
        validateOrder(orderedMigrations, order(2, 1));
    }

    @Test
    void testSimpleCycledGraph() {
        var migrations = List.of(
                migration(Map.of(MigrationParameter.RUN_AFTER, new StringParameterValue("title_2"))),
                migration(Map.of(MigrationParameter.RUN_AFTER, new StringParameterValue("title_1"))));

        Assertions.assertThrowsExactly(IllegalStateException.class, () -> new Graph<>(migrations));
    }

    private LinkedList<Integer> order(Integer... orders) {
        return new LinkedList<>(List.of(orders));
    }

    private void validateOrder(LinkedList<? extends Node> orderedMigrations, LinkedList<Integer> migrationOrder) {
        Assertions.assertEquals(orderedMigrations.size(), migrationOrder.size());
        while (!orderedMigrations.isEmpty()) {
            var node = orderedMigrations.pop();
            var order = migrationOrder.pop();
            Assertions.assertEquals(node.getNaturalOrder(), order);
        }
    }

    private Migration migration() {
        return migration(Map.of());
    }

    private Migration migration(Map<MigrationParameter, ParameterValue> params) {
        migrationCounter++;
        return new Migration.MigrationBuilder()
                .header("title_" + migrationCounter, "author", params, Map.of())
                .build(Path.of("/home/"+ migrationCounter), migrationCounter, "body_" + migrationCounter);
    }
}