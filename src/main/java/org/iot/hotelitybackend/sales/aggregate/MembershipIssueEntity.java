package org.iot.hotelitybackend.sales.aggregate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "membership_issue_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MembershipIssueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int membershipIssueCodePk;
    private int customerCodeFk;
    private Date membershipIssueDate;
    private int membershipLevelCodeFk;
}
