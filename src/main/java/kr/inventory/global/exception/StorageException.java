package kr.inventory.global.exception;

import lombok.Getter;

@Getter
public class StorageException extends RuntimeException {
	private final StorageErrorCode errorCode;

	public StorageException(StorageErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
