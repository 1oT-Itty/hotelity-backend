package org.iot.hotelitybackend.hotelservice.aggregate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.iot.hotelitybackend.employee.aggregate.EmployeeEntity;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "stay_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class StayEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer stayCodePk;
	private LocalDateTime stayCheckinTime;
	private LocalDateTime stayCheckoutTime;
	private Integer stayPeopleCount;

	@Column(name = "employee_code_fk")
	private Integer employeeCode;
	private Integer reservationCodeFk;

	@Builder
	public StayEntity(
		Integer stayCodePk,
		LocalDateTime stayCheckinTime,
		LocalDateTime stayCheckoutTime,
		Integer stayPeopleCount,
		Integer employeeCode,
		Integer reservationCodeFk
	) {
		this.stayCodePk = stayCodePk;
		this.stayCheckinTime = stayCheckinTime;
		this.stayCheckoutTime = stayCheckoutTime;
		this.stayPeopleCount = stayPeopleCount;
		this.employeeCode = employeeCode;
		this.reservationCodeFk = reservationCodeFk;
	}
}
