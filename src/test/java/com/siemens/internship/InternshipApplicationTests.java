package com.siemens.internship;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
public class InternshipApplicationTests {

	@Mock
	private ItemRepository itemRepository;

	@InjectMocks
	private ItemService itemService;

	private Item testItem;

	@BeforeEach
	void setUp() {
		testItem = new Item();
		testItem.setId(1L);
		testItem.setName("Test Item");
		testItem.setDescription("Description");
		testItem.setStatus("NEW");
		testItem.setEmail("test@example.com");
	}

	@Test
	void findAll_ReturnsAllItems() {
		List<Item> items = Collections.singletonList(testItem);
		when(itemRepository.findAll()).thenReturn(items);

		List<Item> result = itemService.findAll();

		assertEquals(1, result.size());
		assertEquals(testItem, result.get(0));
		verify(itemRepository).findAll();
	}

	@Test
	void findAll_ReturnsEmptyList_WhenNoItems() {
		when(itemRepository.findAll()).thenReturn(Collections.emptyList());

		List<Item> result = itemService.findAll();

		assertTrue(result.isEmpty());
		verify(itemRepository).findAll();
	}

	@Test
	void findById_ReturnsItem_WhenFound() {
		when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

		Optional<Item> result = itemService.findById(1L);

		assertTrue(result.isPresent());
		assertEquals(testItem, result.get());
		verify(itemRepository).findById(1L);
	}

	@Test
	void findById_ReturnsEmpty_WhenNotFound() {
		when(itemRepository.findById(1L)).thenReturn(Optional.empty());

		Optional<Item> result = itemService.findById(1L);

		assertFalse(result.isPresent());
		verify(itemRepository).findById(1L);
	}

	@Test
	void save_SavesItem() {
		when(itemRepository.save(any(Item.class))).thenReturn(testItem);

		Item result = itemService.save(testItem);

		assertEquals(testItem, result);
		verify(itemRepository).save(testItem);
	}

	@Test
	void deleteById_DeletesItem_WhenExists() {
		when(itemRepository.existsById(1L)).thenReturn(true);

		itemService.deleteById(1L);

		verify(itemRepository).deleteById(1L);
	}

	@Test
	void deleteById_ThrowsException_WhenNotExists() {
		when(itemRepository.existsById(1L)).thenReturn(false);

		assertThrows(EntityNotFoundException.class, () -> itemService.deleteById(1L));
		verify(itemRepository, never()).deleteById(1L);
	}

	@Test
	void processItemsAsync_ProcessesMultipleItems() throws Exception {
		List<Long> itemIds = Arrays.asList(1L, 2L);
		Item item2 = new Item();
		item2.setId(2L);
		item2.setName("Test Item 2");
		item2.setStatus("NEW");
		item2.setEmail("test2@example.com");

		Item processedItem1 = new Item();
		processedItem1.setId(1L);
		processedItem1.setName("Test Item");
		processedItem1.setStatus("PROCESSED");
		processedItem1.setEmail("test@example.com");

		Item processedItem2 = new Item();
		processedItem2.setId(2L);
		processedItem2.setName("Test Item 2");
		processedItem2.setStatus("PROCESSED");
		processedItem2.setEmail("test2@example.com");

		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
		when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
		when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get();

		assertEquals(2, result.size());
		assertTrue(result.stream().allMatch(item -> "PROCESSED".equals(item.getStatus())));
		verify(itemRepository).findAllIds();
		verify(itemRepository).findById(1L);
		verify(itemRepository).findById(2L);
		verify(itemRepository, times(2)).save(any(Item.class));
	}

	@Test
	void processItemsAsync_ReturnsEmptyList_WhenNoItems() throws Exception {
		when(itemRepository.findAllIds()).thenReturn(Collections.emptyList());

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();
		List<Item> result = future.get();

		assertTrue(result.isEmpty());
		verify(itemRepository).findAllIds();
		verify(itemRepository, never()).findById(any());
		verify(itemRepository, never()).save(any());
	}

	@Test
	void processItemsAsync_ThrowsException_WhenProcessingFails() {
		List<Long> itemIds = Collections.singletonList(1L);
		when(itemRepository.findAllIds()).thenReturn(itemIds);
		when(itemRepository.findById(1L)).thenThrow(new RuntimeException("DB error"));

		CompletableFuture<List<Item>> future = itemService.processItemsAsync();

		assertThrows(Exception.class, future::get);
		verify(itemRepository).findAllIds();
		verify(itemRepository).findById(1L);
	}
}