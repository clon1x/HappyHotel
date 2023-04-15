package com.mockitotutorial.happyhotel.booking;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

	@InjectMocks
	private BookingService bookingService;
	
	@Mock
	private PaymentService paymentServiceMock;
	
	@Mock
	private RoomService roomServiceMock;

	@Spy
	private BookingDAO bookingDAOMock;

	@Mock
	private MailSender mailSenderMock;
	
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
			given(roomServiceMock.getAvailableRooms())
				.willReturn(Collections.singletonList(new Room("101", 2)));
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
			given(roomServiceMock.getAvailableRooms())
				.willReturn(rooms);
			int expected = 6;

			// when
			int actual = bookingService.getAvailablePlaceCount();
			
			// then
			assertEquals(expected, actual);
			
		}
		
		@Test
		void should_CountAvailablePlaces_When_CalledMultipleTimes() {
			
			// given
			given(roomServiceMock.getAvailableRooms())
				.willReturn(Collections.singletonList(new Room("406", 6)))
				.willReturn(Collections.emptyList());
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
			given(roomServiceMock.findAvailableRoomId(Mockito.any(BookingRequest.class)))
				.willThrow(BusinessException.class);
			
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
			
			given(roomServiceMock.findAvailableRoomId(any(BookingRequest.class)))
				.willReturn(AVAILABLE_ROOM_ID);
			
			given(paymentServiceMock.pay(any(BookingRequest.class), anyDouble()))
				.willReturn(PAYMENT_ANSWER);
			
		// when
			String bookingId = bookingService.makeBooking(bookingRequest);
			
		// then
			then(roomServiceMock).should().findAvailableRoomId(bookingRequest);
			then(paymentServiceMock).should().pay(bookingRequest, 400);
			then(bookingDAOMock).should().save(bookingRequest);
			then(roomServiceMock).should().bookRoom(AVAILABLE_ROOM_ID);
			then(mailSenderMock).should().sendBookingConfirmation(bookingId);
			
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
			
			given(roomServiceMock.findAvailableRoomId(any(BookingRequest.class)))
				.willReturn(AVAILABLE_ROOM_ID);
			
			given(bookingDAOMock.save(any()))
				.willReturn(BOOKING_ID);
			
			String expected = BOOKING_ID;
			
		// when
			String actual = bookingService.makeBooking(bookingRequest);
			
		// then
			then(roomServiceMock).should().findAvailableRoomId(bookingRequest);
			then(paymentServiceMock).should(never()).pay(any(), anyDouble());
			then(bookingDAOMock).should().save(bookingRequest);
			then(roomServiceMock).should().bookRoom(AVAILABLE_ROOM_ID);
			then(mailSenderMock).should().sendBookingConfirmation(BOOKING_ID);
			
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
			
			willReturn(bookingRequest).given(bookingDAOMock).get(BOOKING_ID);
			
			// when
			bookingService.cancelBooking(BOOKING_ID);
			
			// then
			then(roomServiceMock).should().unbookRoom(ROOM_ID);
			then(bookingDAOMock).should().delete(BOOKING_ID);
		}
	}
}
