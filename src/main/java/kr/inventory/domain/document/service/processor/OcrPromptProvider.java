package kr.inventory.domain.document.service.processor;

import org.springframework.stereotype.Component;

@Component
public class OcrPromptProvider {

	public String getReceiptPrompt() {
		return """
			Return ONLY JSON. No markdown tags.
			
			 [Fields]
			 - vendorName: vendor name. no store name
			 - date: YYYY-MM-DD.
			 - amount: total grand total (number).
			 - items[]:
			   - name: product name.
			   - quantity: number only.
			   - costPrice: unit price.
			   - totalPrice: line total.
			   - expirationDate: YYYY-MM-DD or null.
			   - rawCapacity: capacity string from name (e.g. "1kg", "500ml") or null.
			
			 [Rules]
			 1. Extract ALL items. No summary.
			 2. Keep original language.
			 3. If value is unknown, return null.
			 4. Ensure strictly valid JSON.
			
			 Example:
			 {"vendorName":"대구청과","date":"2026-02-11","amount":22000,"items":[{"name":"깐마늘 2kg","quantity":1,"costPrice":22000,"totalPrice":22000,"expirationDate":null,"rawCapacity":"2kg"}]}
			""";
	}
}
