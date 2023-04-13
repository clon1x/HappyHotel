package com.mockitotutorial.happyhotel.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BookingServiceTest {

	private BookingService bookingService;
	
	private PaymentService paymentServiceMock;
	private RoomService roomServiceMock;
	private BookingDAO bookingDAOMock;
	private MailSender mailSenderMock;
	
	@BeforeEach
	void setUp() throws Exception {
		this.paymentServiceMock = Mockito.mock(PaymentService.class);
		this.roomServiceMock = Mockito.mock(RoomService.class);
		this.bookingDAOMock = Mockito.mock(BookingDAO.class);
		this.mailSenderMock = Mockito.mock(MailSender.class);
		
		this.bookingService = new BookingService(paymentServiceMock, roomServiceMock, bookingDAOMock, mailSenderMock);
	}

	@Nested
	class CalculatePriceTests {
		@Test
		void should_CalculateCorrectPrice_When_CorrectInput() {
			
			// given
			BookingRequest bookingRequest = new BookingRequest("1",
															   LocalDate.of(2023, 01, 01),
															   LocalDate.of(2023, 01, 05), 
															   2, 
															   false);
			double expected = 400.0;
			
			// when
			double actual = bookingService.calculatePrice(bookingRequest);
			
			// then
			assertEquals(expected, actual);
			
		}
	}
	
	@Nested
	class GetAvailablePlaceCountTests {
		
		@Test
		void should_ReturnZero_When_NoRoomsAvailable() {
			
			// given
			int expected = 0;
			
			// when
			int actual = bookingService.getAvailablePlaceCount();
			
			// then
			assertEquals(expected, actual);
			
		}
		
		@Test
		void should_CountAvailablePlaces_When_OneRoomAvailable() {
			
			// given
			Mockito.when(roomServiceMock.getAvailableRooms()).thenReturn(Collections.singletonList(new Room("101", 2)));
			int expected = 2;
			
			// when
			int actual = bookingService.getAvailablePlaceCount();
			
			// then
			assertEquals(expected, actual);
			
		}
		
		@Test
		void should_CountCorrectPlaces_When_MultipleRoomsAvailable() {
			
			// given
			List<Room> rooms = Arrays.asList(new Room("101",2), new Room("102",3), new Room("105",1));
			Mockito.when(roomServiceMock.getAvailableRooms())
				.thenReturn(rooms);
			int expected = 6;

			// when
			int actual = bookingService.getAvailablePlaceCount();
			
			// then
			assertEquals(expected, actual);
			
		}
		
	}

}
