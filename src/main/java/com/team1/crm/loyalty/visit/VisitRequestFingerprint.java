package com.team1.crm.loyalty.visit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** 입력 경계를 길이로 구분해 원본 요청의 지문을 만든다. 빈 방문 시각은 재시도 시각과 무관하게 동일하다. */
final class VisitRequestFingerprint {
    private VisitRequestFingerprint() {}
    static String of(VisitForm form) {
        try {
            var bytes=new ByteArrayOutputStream();
            var out=new DataOutputStream(bytes);
            out.writeInt(form.getAmount());
            out.writeInt(form.getUsedPoints()==null?0:form.getUsedPoints());
            write(out,form.getCouponId()==null?null:form.getCouponId().toString());
            write(out,form.getVisitedAt()==null?null:form.getVisitedAt().toString());
            write(out,form.getMenuMemo());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException|NoSuchAlgorithmException error){throw new IllegalStateException("방문 요청 검증 실패",error);}
    }
    private static void write(DataOutputStream out,String value)throws IOException {
        if(value==null){out.writeInt(-1);return;}
        byte[] bytes=value.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);
    }
}
