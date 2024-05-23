package org.iot.hotelitybackend.customer.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.iot.hotelitybackend.customer.aggregate.CustomerEntity;
import org.iot.hotelitybackend.customer.aggregate.CustomerSpecification;
import org.iot.hotelitybackend.customer.dto.CustomerDTO;
import org.iot.hotelitybackend.customer.repository.CustomerRepository;
import org.iot.hotelitybackend.customer.repository.NationRepository;
import org.iot.hotelitybackend.sales.aggregate.MembershipEntity;
import org.iot.hotelitybackend.sales.aggregate.MembershipIssueEntity;
import org.iot.hotelitybackend.customer.aggregate.NationEntity;
import org.iot.hotelitybackend.sales.dto.MembershipDTO;
import org.iot.hotelitybackend.sales.repository.MembershipIssueRepository;
import org.iot.hotelitybackend.sales.repository.MembershipRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.iot.hotelitybackend.common.constant.Constant.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {
	private final ModelMapper mapper;
	private final CustomerRepository customerRepository;
	private final NationRepository nationRepository;
	private final MembershipRepository membershipRepository;
	private final MembershipIssueRepository membershipIssueRepository;

	@Autowired
	public CustomerServiceImpl(ModelMapper mapper, CustomerRepository customerRepository,
		NationRepository nationRepository,
		MembershipRepository membershipRepository, MembershipIssueRepository membershipIssueRepository) {
		this.mapper = mapper;
		this.customerRepository = customerRepository;
		this.nationRepository = nationRepository;
		this.membershipRepository = membershipRepository;
		this.membershipIssueRepository = membershipIssueRepository;
	}

	@Override
	public Map<String, Object> selectCustomersList(Integer customerCodePk, String customerName, String customerEmail,
		String customerPhoneNumber, String customerEnglishName, String customerAddress, Integer customerInfoAgreement, Integer customerStatus,
		Date customerRegisteredDate, Integer nationCodeFk, String customerGender, String nationName, String customerType,
		String membershipLevelName, String orderBy, Integer sortBy, Integer pageNum) {

		Pageable pageable;

		if(orderBy == null){
			pageable = PageRequest.of(pageNum, PAGE_SIZE, Sort.by("customerCodePk"));
		} else{
			if (sortBy == 1){
				pageable = PageRequest.of(pageNum, PAGE_SIZE, Sort.by(orderBy));
			}
			else{
				pageable = PageRequest.of(pageNum, PAGE_SIZE, Sort.by(orderBy).descending());
			}
		}

		Specification<CustomerEntity> spec = spec(customerCodePk, customerName, customerEmail, customerPhoneNumber, customerEnglishName,
			customerAddress, customerInfoAgreement, customerStatus, customerRegisteredDate, nationCodeFk,
			customerGender, nationName, customerType, membershipLevelName);

		// 필터 조건에 따라 고객 정보 조회
		Page<CustomerEntity> customerPage = customerRepository.findAll(spec, pageable);
		List<CustomerDTO> customerDTOList = customerPage.stream()
			.map(customerEntity -> mapper.map(customerEntity, CustomerDTO.class))
			.peek(customerDTO -> {
				customerDTO.setNationName(nationRepository.findById(customerDTO.getNationCodeFk())
					.map(NationEntity::getNationName)
					.orElse(null));
				MembershipIssueEntity issue = membershipIssueRepository.findByCustomerCodeFk(
					customerDTO.getCustomerCodePk());
				if (issue != null) {
					customerDTO.setMembershipLevelName(membershipRepository.findById(issue.getMembershipLevelCodeFk())
						.map(MembershipEntity::getMembershipLevelName)
						.orElse(null));
				} else {
					customerDTO.setMembershipLevelName(null); // issue가 null인 경우 null로 설정
				}
			})
			.collect(Collectors.toList());

		Map<String, Object> customerPageInfo = new HashMap<>();
		customerPageInfo.put(KEY_TOTAL_PAGES_COUNT, customerPage.getTotalPages());
		customerPageInfo.put(KEY_CURRENT_PAGE_INDEX, customerPage.getNumber());
		customerPageInfo.put(KEY_CONTENT, customerDTOList);

		return customerPageInfo;
	}

	@Override
	public CustomerDTO selectCustomerByCustomerCodePk(int customerCodePk) {

		CustomerEntity customerEntity = customerRepository.findById(customerCodePk).get();

		List<MembershipIssueEntity> membershipIssueEntities = membershipIssueRepository.findMembershipByCustomerCodeFk(customerCodePk);

		CustomerDTO customerDTO = mapper.map(customerEntity, CustomerDTO.class);

		List<MembershipDTO> membershipDTOs = membershipIssueEntities.stream()
				.map(membershipIssue -> {
					MembershipDTO membershipDTO = new MembershipDTO();
					membershipDTO.setMembershipLevelName(membershipIssue.getMembership().getMembershipLevelName());
					return membershipDTO;
				}).collect(Collectors.toList());

		customerDTO.setMemberships(membershipDTOs);

		return customerDTO;
	}

	@Override
	public Map<String, Object> readExcel(Workbook workbook) {
		Sheet worksheet = workbook.getSheetAt(0);

		for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
			Row row = worksheet.getRow(i);

			CustomerEntity customerEntity = CustomerEntity.builder()
				.customerName(row.getCell(0).getStringCellValue())
				.customerEmail(row.getCell(1).getStringCellValue())
				.customerPhoneNumber(row.getCell(2).getStringCellValue())
				.customerEnglishName(row.getCell(3).getStringCellValue())
				.customerAddress(row.getCell(4).getStringCellValue())
				.customerInfoAgreement((int)row.getCell(5).getNumericCellValue())
				.customerStatus((int)row.getCell(6).getNumericCellValue())
				.customerRegisteredDate(new Date())
				.customerType(row.getCell(8).getStringCellValue())
				.nationCodeFk((int)row.getCell(9).getNumericCellValue())
				.customerGender(row.getCell(10).getStringCellValue())
				.build();
			customerRepository.save(customerEntity);
		}

		Map<String, Object> modifiedCustomerInfo = new HashMap<>();
		modifiedCustomerInfo.put(KEY_CONTENT, "success");
		return modifiedCustomerInfo;
	}

	@Override
	@Transactional
	public Map<String, Object> deleteCustomerByCustomerCodePk(int customerCodePk) {


		CustomerEntity customerEntity = customerRepository.findById(customerCodePk).get();
		CustomerEntity customer = CustomerEntity.builder()
			.customerCodePk(customerCodePk)
			.customerName(customerEntity.getCustomerName())
			.customerEmail(customerEntity.getCustomerEmail())
			.customerPhoneNumber(customerEntity.getCustomerPhoneNumber())
			.customerEnglishName(customerEntity.getCustomerEnglishName())
			.customerAddress(customerEntity.customerAddress)
			.customerInfoAgreement(customerEntity.getCustomerInfoAgreement())
			.customerStatus(0)
			.customerRegisteredDate(customerEntity.getCustomerRegisteredDate())
			.customerType(customerEntity.getCustomerType())
			.nationCodeFk(customerEntity.getNationCodeFk())
			.customerGender(customerEntity.getCustomerGender())
			.build();
		customerRepository.save(customer);

		Map<String, Object> modifiedCustomerInfo = new HashMap<>();
		modifiedCustomerInfo.put(KEY_CONTENT, "success");
		return modifiedCustomerInfo;
	}

	@Override
	public ByteArrayInputStream downloadExcel(Integer customerCodePk, String customerName, String customerEmail,
		String customerPhoneNumber, String customerEnglishName, String customerAddress, Integer customerInfoAgreement,
		Integer customerStatus, Date customerRegisteredDate, Integer nationCodeFk, String customerGender,
		String nationName, String customerType, String membershipLevelName) throws IOException {

		Specification<CustomerEntity> spec = spec(customerCodePk, customerName, customerEmail, customerPhoneNumber, customerEnglishName,
			customerAddress, customerInfoAgreement, customerStatus, customerRegisteredDate, nationCodeFk,
			customerGender, nationName, customerType, membershipLevelName);

		// 필터 조건에 따라 고객 정보 조회
		List<CustomerEntity> customerPage = customerRepository.findAll(spec);
		List<CustomerDTO> customerDTOList = customerPage.stream()
			.map(customerEntity -> mapper.map(customerEntity, CustomerDTO.class))
			.peek(customerDTO -> {
				customerDTO.setNationName(nationRepository.findById(customerDTO.getNationCodeFk())
					.map(NationEntity::getNationName)
					.orElse(null));
				MembershipIssueEntity issue = membershipIssueRepository.findByCustomerCodeFk(
					customerDTO.getCustomerCodePk());
				if (issue != null) {
					customerDTO.setMembershipLevelName(membershipRepository.findById(issue.getMembershipLevelCodeFk())
						.map(MembershipEntity::getMembershipLevelName)
						.orElse(null));
				} else {
					customerDTO.setMembershipLevelName(null); // issue가 null인 경우 null로 설정
				}
			})
			.collect(Collectors.toList());


		Workbook workbook = new XSSFWorkbook();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor(IndexedColors.BLACK.getIndex());

		CellStyle headerCellStyle = workbook.createCellStyle();
		headerCellStyle.setFont(headerFont);
		headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		Sheet customerSheet = workbook.createSheet("고객");

		createDashboardSheet(customerDTOList, customerSheet, headerCellStyle);

		workbook.write(out);
		log.info("[ReportService:getCustomerToExcel] create Customer list done. row count:[{}]", customerDTOList.size());

		return new ByteArrayInputStream(out.toByteArray());
	}

	private void createDashboardSheet(List<CustomerDTO> customer, Sheet customerSheet, CellStyle headerCellStyle) {
		Row headerRow = customerSheet.createRow(0);
		String[] headerStrings = {"고객코드", "고객타입",	"국가",	"영문 이름"	, "한글 이름",	"성별", "이메일"
			, "전화번호", "주소", "멤버십 등급"};

		int idx = 0;
		Cell headerCell = null;
		for(String s: headerStrings){
			headerCell = headerRow.createCell(idx++);
			headerCell.setCellValue(s);
			headerCell.setCellStyle(headerCellStyle);
		}

		Row bodyRow = null;
		Cell bodyCell = null;
		int index = 1;
		for(CustomerDTO data: customer){
			bodyRow = customerSheet.createRow(index++);
			bodyCell = bodyRow.createCell(0);
			bodyCell.setCellValue(data.getCustomerCodePk());
			bodyCell = bodyRow.createCell(1);
			bodyCell.setCellValue(data.getCustomerType());
			bodyCell = bodyRow.createCell(2);
			bodyCell.setCellValue(data.getNationName());
			bodyCell = bodyRow.createCell(3);
			bodyCell.setCellValue(data.getCustomerEnglishName());
			bodyCell = bodyRow.createCell(4);
			bodyCell.setCellValue(data.getCustomerName());
			bodyCell = bodyRow.createCell(5);
			bodyCell.setCellValue(data.getCustomerGender());
			bodyCell = bodyRow.createCell(6);
			bodyCell.setCellValue(data.getCustomerEmail());
			bodyCell = bodyRow.createCell(7);
			bodyCell.setCellValue(data.getCustomerPhoneNumber());
			bodyCell = bodyRow.createCell(8);
			bodyCell.setCellValue(data.getCustomerAddress());
			bodyCell = bodyRow.createCell(9);
			bodyCell.setCellValue(data.getMembershipLevelName());
		}

		for (int i = 0; i < headerStrings.length; i++) {
			customerSheet.autoSizeColumn(i);
			customerSheet.setColumnWidth(i, (customerSheet.getColumnWidth(i)) + (short)1024);
		}

	}

	private Specification<CustomerEntity> spec(Integer customerCodePk, String customerName, String customerEmail,
		String customerPhoneNumber, String customerEnglishName, String customerAddress, Integer customerInfoAgreement,
		Integer customerStatus, Date customerRegisteredDate, Integer nationCodeFk, String customerGender,
		String nationName, String customerType, String membershipLevelName){


		Specification<CustomerEntity> spec = Specification.where(null);

		if(customerCodePk != null){
			spec = spec.and(CustomerSpecification.equalsCustomerCodePk(customerCodePk));
		}
		if (customerName != null && !customerName.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerName(customerName));
		}
		if (customerEmail != null && !customerEmail.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerEmail(customerEmail));
		}
		if (customerPhoneNumber != null && !customerPhoneNumber.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerPhoneNumber(customerPhoneNumber));
		}
		if (customerEnglishName != null && !customerEnglishName.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerEnglishName(customerEnglishName));
		}
		if (customerAddress != null && !customerAddress.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerAddress(customerAddress));
		}
		if(customerInfoAgreement != null){
			spec = spec.and(CustomerSpecification.equalsCustomerInfoAgreement(customerInfoAgreement));
		}
		if(customerStatus != null){
			spec = spec.and(CustomerSpecification.equalsCustomerStatus(customerStatus));
		}
		if(customerRegisteredDate != null){
			spec = spec.and(CustomerSpecification.equalsCustomerRegisteredDate(customerRegisteredDate));
		}
		if(nationCodeFk != null){
			spec = spec.and(CustomerSpecification.equalsNationCodeFk(nationCodeFk));
		}
		if (customerGender != null && !customerGender.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerGender(customerGender));
		}
		if (nationName != null && !nationName.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsNationName(nationName));
		}
		// 멤버십 레벨 이름으로 필터링
		if (membershipLevelName != null && !membershipLevelName.isEmpty()) {
			MembershipEntity membership = membershipRepository.findByMembershipLevelName(membershipLevelName);
			if (membership != null) {
				spec = spec.and(CustomerSpecification.equalsMembershipLevelName(membershipLevelName));
			}
		}
		// 고객 유형으로 필터링
		if (customerType != null && !customerType.isEmpty()) {
			spec = spec.and(CustomerSpecification.equalsCustomerType(customerType));
		}

		return spec;
	}
}