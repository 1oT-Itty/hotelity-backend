package org.iot.hotelitybackend.hotelservice.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.iot.hotelitybackend.common.vo.ResponseVO;
import org.iot.hotelitybackend.hotelservice.service.StayService;
import org.iot.hotelitybackend.hotelservice.vo.RequestRegistStay;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hotel-service")
public class StayController {

	private final StayService stayService;
	private final ModelMapper mapper;

	@Autowired
	public StayController(StayService stayService, ModelMapper mapper) {
		this.stayService = stayService;
		this.mapper = mapper;
	}

	/* 투숙 전체 내역 조회 (다중 조건 검색) */
	@GetMapping("/stays/page")
	public ResponseEntity<ResponseVO> selectStaysList(
		@RequestParam int pageNum,
		@RequestParam(required = false) String branchCodeFk,
		@RequestParam(required = false) String roomCodeFk,
		@RequestParam(required = false) LocalDateTime reservationCheckinDate,
		@RequestParam(required = false) LocalDateTime reservationCheckoutDate
	) {

		Map<String, Object> stayListInfo =
			stayService.selectStaysList(pageNum, branchCodeFk, roomCodeFk, reservationCheckinDate, reservationCheckoutDate);

		ResponseVO response = ResponseVO.builder()
			.data(stayListInfo)
			.resultCode(HttpStatus.OK.value())
			.message("조회 성공")
			.build();

		return ResponseEntity.status(response.getResultCode()).body(response);
	}

	/* 고객 이름별 투숙 내역 조회 */
	@GetMapping("/stays/customers")
	public ResponseEntity<ResponseVO> selectStaysListByCustomerName(@RequestParam String customerName) {

		Map<String, Object> stayInfo = stayService.selectStaysListByCustomerName(customerName);

		ResponseVO response = ResponseVO.builder()
			.data(stayInfo)
			.resultCode(HttpStatus.OK.value())
			.message(customerName + " 고객님 투숙 내역 조회")
			.build();

		return ResponseEntity.status(response.getResultCode()).body(response);
	}

	/* 예약 체크인 선택 시 투숙 정보 생성 */
	@PostMapping("/stays")
	public ResponseEntity<ResponseVO> registStayByReservationCodePk(
		@RequestParam int reservationCodePk,
		@RequestParam int employeeCodeFk) {

		Map<String, Object> registStayInfo = stayService.registStayByReservationCodePk(reservationCodePk, employeeCodeFk);

		if (registStayInfo.isEmpty()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
		} else {
			ResponseVO response = ResponseVO.builder()
				.data(registStayInfo)
				.resultCode(HttpStatus.CREATED.value())
				.message("예약 코드 " + reservationCodePk + "번 투숙 등록됨")
				.build();
			return ResponseEntity.status(response.getResultCode()).body(response);
		}
	}

	/* 투숙 체크아웃 */
	/* 화면에서 체크아웃 할 투숙 내역을 체크 후 버튼을 눌렀을 때 */
	@PutMapping("/stays/{stayCodePk}/checkout")
	public ResponseEntity<ResponseVO> modifyStayCheckoutDate(@PathVariable("stayCodePk") Integer stayCodePk) {

		Map<String, Object> checkoutStayInfo = stayService.modifyStayCheckoutDate(stayCodePk);

		ResponseVO response = ResponseVO.builder()
			.data(checkoutStayInfo)
			.resultCode(HttpStatus.OK.value())
			.message(stayCodePk + "번 투숙 체크아웃 완료")
			.build();

		return ResponseEntity.status(response.getResultCode()).body(response);
	}
}
