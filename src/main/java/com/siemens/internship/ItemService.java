package com.siemens.internship;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    /**
     * @throws EntityNotFoundException if item is not found
     */

    public void deleteById(Long id) {

        if(!itemRepository.existsById(id)) {
            throw new EntityNotFoundException("Item with id " + id + " not found");
        }
        itemRepository.deleteById(id);
    }

    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     * <p>
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */



    /**
     * Asynchronously processes all items in the database by:
     * Fetching their IDs
     * Retrieving each item from the repository
     * Updating its status to "PROCESSED"
     * Saving the updated item back to the database
     * Collecting successfully processed items into a list
     * <p>
     * Each item is processed in parallel using CompletableFuture, and the method waits
     * for all tasks to complete before returning the final result.
     * <p>
     * @return CompletableFuture<ArrayList<Item>> containing all successfully processed items
     * @throws IllegalStateException if any error occurs during asynchronous processing
     */

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();
        Queue<Item> processedItems = new ConcurrentLinkedQueue<>();

        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        Optional<Item> itemOpt = itemRepository.findById(id);
                        if (itemOpt.isPresent()) {
                            Item item = itemOpt.get();
                            item.setStatus("PROCESSED");
                            itemRepository.save(item);
                            processedItems.add(item);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to process item ID: " + id, e);
                    }
                }))
                .toList();

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> List.copyOf(processedItems));
    }
}