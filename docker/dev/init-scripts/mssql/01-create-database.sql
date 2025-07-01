-- MSSQL 개발환경 초기화 스크립트
USE master;
GO

-- 데이터베이스 생성
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'sourceDB')
BEGIN
    CREATE DATABASE sourceDB;
END
GO

USE sourceDB;
GO

-- 한글 데이터가 포함된 예시 테이블들 생성

-- 1. 사용자 테이블
CREATE TABLE 사용자 (
    사용자ID INT IDENTITY(1,1) PRIMARY KEY,
    이름 NVARCHAR(50) NOT NULL,
    이메일 NVARCHAR(100) UNIQUE NOT NULL,
    전화번호 NVARCHAR(20),
    주소 NVARCHAR(200),
    생년월일 DATE,
    성별 NVARCHAR(10),
    직업 NVARCHAR(50),
    등록일시 DATETIME2 DEFAULT GETDATE(),
    수정일시 DATETIME2 DEFAULT GETDATE(),
    활성여부 BIT DEFAULT 1
);

-- 2. 상품 테이블
CREATE TABLE 상품 (
    상품ID INT IDENTITY(1,1) PRIMARY KEY,
    상품명 NVARCHAR(100) NOT NULL,
    상품설명 NVARCHAR(500),
    카테고리 NVARCHAR(50),
    가격 DECIMAL(10,2) NOT NULL,
    재고수량 INT DEFAULT 0,
    제조사 NVARCHAR(100),
    원산지 NVARCHAR(50),
    등록일시 DATETIME2 DEFAULT GETDATE(),
    수정일시 DATETIME2 DEFAULT GETDATE(),
    판매상태 NVARCHAR(20) DEFAULT '판매중'
);

-- 3. 주문 테이블
CREATE TABLE 주문 (
    주문ID INT IDENTITY(1,1) PRIMARY KEY,
    사용자ID INT NOT NULL,
    주문번호 NVARCHAR(50) UNIQUE NOT NULL,
    주문일시 DATETIME2 DEFAULT GETDATE(),
    총금액 DECIMAL(12,2) NOT NULL,
    배송주소 NVARCHAR(200),
    주문상태 NVARCHAR(20) DEFAULT '주문접수',
    결제방법 NVARCHAR(20),
    배송메모 NVARCHAR(200),
    FOREIGN KEY (사용자ID) REFERENCES 사용자(사용자ID)
);

-- 4. 주문상세 테이블
CREATE TABLE 주문상세 (
    주문상세ID INT IDENTITY(1,1) PRIMARY KEY,
    주문ID INT NOT NULL,
    상품ID INT NOT NULL,
    수량 INT NOT NULL,
    단가 DECIMAL(10,2) NOT NULL,
    소계 DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (주문ID) REFERENCES 주문(주문ID),
    FOREIGN KEY (상품ID) REFERENCES 상품(상품ID)
);

-- 5. 카테고리 테이블
CREATE TABLE 카테고리 (
    카테고리ID INT IDENTITY(1,1) PRIMARY KEY,
    카테고리명 NVARCHAR(50) NOT NULL,
    상위카테고리ID INT NULL,
    카테고리설명 NVARCHAR(200),
    정렬순서 INT DEFAULT 0,
    사용여부 BIT DEFAULT 1,
    등록일시 DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (상위카테고리ID) REFERENCES 카테고리(카테고리ID)
);

-- 6. 리뷰 테이블
CREATE TABLE 리뷰 (
    리뷰ID INT IDENTITY(1,1) PRIMARY KEY,
    상품ID INT NOT NULL,
    사용자ID INT NOT NULL,
    평점 INT CHECK (평점 >= 1 AND 평점 <= 5),
    제목 NVARCHAR(100),
    내용 NVARCHAR(1000),
    작성일시 DATETIME2 DEFAULT GETDATE(),
    수정일시 DATETIME2 DEFAULT GETDATE(),
    추천수 INT DEFAULT 0,
    FOREIGN KEY (상품ID) REFERENCES 상품(상품ID),
    FOREIGN KEY (사용자ID) REFERENCES 사용자(사용자ID)
);

-- 7. 공지사항 테이블
CREATE TABLE 공지사항 (
    공지ID INT IDENTITY(1,1) PRIMARY KEY,
    제목 NVARCHAR(200) NOT NULL,
    내용 NVARCHAR(MAX),
    작성자 NVARCHAR(50),
    작성일시 DATETIME2 DEFAULT GETDATE(),
    조회수 INT DEFAULT 0,
    중요여부 BIT DEFAULT 0,
    게시시작일 DATE,
    게시종료일 DATE,
    첨부파일명 NVARCHAR(200)
);

-- 8. 쿠폰 테이블
CREATE TABLE 쿠폰 (
    쿠폰ID INT IDENTITY(1,1) PRIMARY KEY,
    쿠폰명 NVARCHAR(100) NOT NULL,
    쿠폰코드 NVARCHAR(50) UNIQUE NOT NULL,
    할인타입 NVARCHAR(20), -- '정액할인', '정률할인'
    할인값 DECIMAL(10,2),
    최소주문금액 DECIMAL(10,2),
    최대할인금액 DECIMAL(10,2),
    발급시작일 DATE,
    발급종료일 DATE,
    사용기한 DATE,
    발급수량 INT,
    사용수량 INT DEFAULT 0,
    사용여부 BIT DEFAULT 1
);

-- 9. 배송 테이블
CREATE TABLE 배송 (
    배송ID INT IDENTITY(1,1) PRIMARY KEY,
    주문ID INT NOT NULL,
    택배사 NVARCHAR(50),
    송장번호 NVARCHAR(50),
    배송상태 NVARCHAR(20) DEFAULT '배송준비',
    발송일시 DATETIME2,
    배송완료일시 DATETIME2,
    수령인 NVARCHAR(50),
    배송주소 NVARCHAR(200),
    배송메모 NVARCHAR(200),
    FOREIGN KEY (주문ID) REFERENCES 주문(주문ID)
);

-- 10. 문의 테이블
CREATE TABLE 문의 (
    문의ID INT IDENTITY(1,1) PRIMARY KEY,
    사용자ID INT NOT NULL,
    문의유형 NVARCHAR(50), -- '상품문의', '배송문의', '기타문의'
    제목 NVARCHAR(200) NOT NULL,
    내용 NVARCHAR(1000),
    작성일시 DATETIME2 DEFAULT GETDATE(),
    답변내용 NVARCHAR(1000),
    답변일시 DATETIME2,
    답변자 NVARCHAR(50),
    처리상태 NVARCHAR(20) DEFAULT '접수',
    비밀여부 BIT DEFAULT 0,
    FOREIGN KEY (사용자ID) REFERENCES 사용자(사용자ID)
);

PRINT 'MSSQL 개발환경 테이블 생성 완료';
GO
