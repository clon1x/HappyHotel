package com.mockitotutorial.happyhotel.booking;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
		this.bookingDAOMock = Mockito.spy(BookingDAO.class);
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
		
		@Test
		void should_CountAvailablePlaces_When_CalledMultipleTimes() {
			
			// given
			Mockito.when(roomServiceMock.getAvailableRooms())
				.thenReturn(Collections.singletonList(new Room("406", 6)))
				.thenReturn(Collections.emptyList());
			int expectedFirstCall = 6;
			int expectedSecondCall = 0;

			// when
			int actualFirstCall = bookingService.getAvailablePlaceCount();
			int actualSecondCall = bookingService.getAvailablePlaceCount();
			
			// then
			assertAll(
					() -> assertEquals(expectedFirstCall, actualFirstCall),
					() -> assertEquals(expectedSecondCall, actualSecondCall));
			
		}
	}

	@Nested
	class MakeBookingTests {
		@Test
		void should_ThrowBusinessException_When_NoRoomAvailable() {
			
			// given
			BookingRequest bookingRequest = new BookingRequest("1",
															   LocalDate.of(2023, 01, 01),
															   LocalDate.of(2023, 01, 05), 
															   2, 
															   false);
			Mockito.when(roomServiceMock.findAvailableRoomId(Mockito.any(BookingRequest.class)))
				.thenThrow(BusinessException.class);
			
			// when
			Executable makeBooking = () -> bookingService.makeBooking(bookingRequest);
			
			// then
			assertThrows(BusinessException.class, makeBooking);
			
		}

		@Test
		void should_MakeBooking_When_RequestIsPrepaid() {
			
		// given
			BookingRequest bookingRequest = new BookingRequest("1",
															   LocalDate.of(2023, 01, 01),
															   LocalDate.of(2023, 01, 05), 
															   2, 
															   true);
			final String AVAILABLE_ROOM_ID = "101";
			final String PAYMENT_ANSWER = "OK BBVA AuthCode 1djklñzx7890v807897r891";
			
			when(roomServiceMock.findAvailableRoomId(any(BookingRequest.class)))
				.thenReturn(AVAILABLE_ROOM_ID);
			
			when(paymentServiceMock.pay(any(BookingRequest.class), anyDouble()))
				.thenReturn(PAYMENT_ANSWER);
			
		// when
			String bookingId = bookingService.makeBooking(bookingRequest);
			
		// then
			Mockito.verify(roomServiceMock).findAvailableRoomId(bookingRequest);
			Mockito.verify(paymentServiceMock).pay(bookingRequest, 400);
			Mockito.verify(bookingDAOMock).save(bookingRequest);
			Mockito.verify(roomServiceMock).bookRoom(AVAILABLE_ROOM_ID);
			Mockito.verify(mailSenderMock).sendBookingConfirmation(bookingId);
			
			System.out.println("bookingId = " + bookingId );
		}	

		@Test
		void should_MakeBooking_When_RequestIsNotPrepaid() {
			
		// given
			BookingRequest bookingRequest = new BookingRequest("1",
															   LocalDate.of(2023, 01, 01),
															   LocalDate.of(2023, 01, 05), 
															   2, 
															   false);
			final String AVAILABLE_ROOM_ID = "101";
			final String BOOKING_ID = "748921";
			final String PAYMENT_ANSWER = "OK BBVA AuthCode 1djklñzx7890v807897r891";
			
			when(roomServiceMock.findAvailableRoomId(any(BookingRequest.class)))
				.thenReturn(AVAILABLE_ROOM_ID);
			
			when(paymentServiceMock.pay(any(BookingRequest.class), anyDouble()))
				.thenReturn(PAYMENT_ANSWER);
			
			when(bookingDAOMock.save(any())).thenReturn(BOOKING_ID);
			
			String expected = BOOKING_ID;
			
		// when
			String actual = bookingService.makeBooking(bookingRequest);
			
		// then
			Mockito.verify(roomServiceMock).findAvailableRoomId(bookingRequest);
			Mockito.verify(paymentServiceMock, never()).pay(any(), anyDouble());
			Mockito.verify(bookingDAOMock).save(bookingRequest);
			Mockito.verify(roomServiceMock).bookRoom(AVAILABLE_ROOM_ID);
			Mockito.verify(mailSenderMock).sendBookingConfirmation(BOOKING_ID);
			
			assertAll(
				() -> assertEquals(expected, actual)
			);
		}
		
		@Test
		void should_CancelBooking_When_InputOK() {
			
			// given
			final String ROOM_ID = "1.3";
			final String BOOKING_ID = "1";
			
			BookingRequest bookingRequest = new BookingRequest("1",
															   LocalDate.of(2023, 01, 01),
															   LocalDate.of(2023, 01, 05), 
															   2, 
															   false);	
			bookingRequest.setRoomId(ROOM_ID);
			
			doReturn(bookingRequest).when(bookingDAOMock).get(BOOKING_ID);
			
			// when
			bookingService.cancelBooking(BOOKING_ID);
			
			// then
			Mockito.verify(roomServiceMock).unbookRoom(ROOM_ID);
			Mockito.verify(bookingDAOMock).delete(BOOKING_ID);
		}
		
		@Test
		void should_RethrowException_When_PriceIsTooHigh() {
			
		// given
			BookingRequest bookingRequest = new BookingRequest("1",
															   LocalDate.of(2023, 01, 01),
															   LocalDate.of(2023, 01, 05), 
															   2, 
															   true);
			final String AVAILABLE_ROOM_ID = "101";

			when(roomServiceMock.findAvailableRoomId(any(BookingRequest.class)))
				.thenReturn(AVAILABLE_ROOM_ID);
			
			when(paymentServiceMock.pay(any(BookingRequest.class), anyDouble()))
				.thenThrow(new UnsupportedOperationException("Only small payments are supported."));
			
		// when
			Executable makeBooking = () -> bookingService.makeBooking(bookingRequest);
			
		// then
			assertThrows(UnsupportedOperationException.class, makeBooking);
			
		}	
	
	}
}
