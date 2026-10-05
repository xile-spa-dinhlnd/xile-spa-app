#!/usr/bin/env python3
"""Sinh sơ đồ ERD (Mermaid) trực tiếp từ database thật, để sơ đồ luôn khớp với schema.

Cách dùng (cần psql truy cập được vào database đã chạy V1, V2 và later-releases.sql):

    PSQL_CMD='psql -h localhost -U xile -d xile_full' python3 gen_erd_mermaid.py ../diagrams

Mỗi nhóm bảng ghi ra một tệp .mmd trong thư mục đích (mặc định: ./diagrams).
Khi đổi schema, chạy lại script rồi dán lại các khối Mermaid vào docs/design/erd.md.
"""
import os
import subprocess
import sys

PSQL = os.environ.get("PSQL_CMD", "psql")

# Nhóm bảng theo miền nghiệp vụ. Bảng đánh dấu PLANNED là bảng thiết kế cho R2/R3 (chưa có trong V1).
GROUPS = {
    "auth":     ("Xác thực", ["app_user", "refresh_token", "password_reset_token"]),
    "catalog":  ("Danh mục: dịch vụ, combo, nhân viên",
                 ["service_group", "service", "service_price_history", "combo", "combo_item", "staff"]),
    "customer": ("Khách hàng và gói liệu trình",
                 ["customer_source", "customer", "customer_package", "customer_contact"]),
    "sales":    ("Giao dịch, chi phí, chốt sổ, bút toán",
                 ["visit", "visit_item", "expense_category", "expense", "daily_closing", "adjustment"]),
    "system":   ("Hệ thống và đồng bộ", ["audit_log", "sync_job", "app_setting"]),
    "payroll":  ("Tiền công và quảng cáo (R3)", ["staff_payment", "marketing_spend"]),
}

COMMENTS = {
    ("app_user", "password_hash"): "băm",
    ("app_user", "role"): "OWNER/STAFF",
    ("refresh_token", "token_hash"): "băm",
    ("password_reset_token", "token_hash"): "băm",
    ("service", "name"): "duy nhất, chuẩn hóa",
    ("service", "list_price"): "đồng",
    ("service", "tour_fee"): "đồng, mặc định",
    ("combo", "price"): "đồng",
    ("combo", "tour_fee"): "đồng, tạm theo combo",
    ("staff", "role"): "OWNER/THERAPIST",
    ("customer", "phone"): "E.164, duy nhất",
    ("customer", "health_notes"): "nhạy cảm",
    ("customer", "care_status"): "NEW/CARING/...",
    ("customer", "anonymized_at"): "ẩn danh",
    ("visit", "business_date"): "ngày làm việc",
    ("visit", "payment_method"): "CASH/TRANSFER",
    ("visit", "status"): "ACTIVE/VOID",
    ("visit_item", "item_type"): "SERVICE/COMBO/PACKAGE_SALE/PACKAGE_USE",
    ("visit_item", "name_snapshot"): "chụp lại",
    ("visit_item", "list_price_snapshot"): "chụp lại, đồng",
    ("visit_item", "discount_percent"): "0 đến 100",
    ("visit_item", "tour_fee_snapshot"): "chụp lại, đồng",
    ("expense_category", "kind"): "PURCHASE/CTV/OTHER",
    ("expense", "amount"): "đồng, lớn hơn 0",
    ("daily_closing", "business_date"): "có hàng = đã chốt",
    ("daily_closing", "total_revenue"): "sinh tự động",
    ("daily_closing", "total_expense"): "sinh tự động",
    ("daily_closing", "net_received"): "sinh tự động",
    ("adjustment", "recorded_on"): "ngày ghi nhận",
    ("adjustment", "original_date"): "ngày gốc",
    ("adjustment", "kind"): "REVENUE/EXPENSE",
    ("adjustment", "amount"): "có dấu, khác 0",
    ("adjustment", "voids_visit"): "loại lượt đến (OQ-16)",
    ("audit_log", "before_data"): "chỉ thêm",
    ("sync_job", "business_date"): "ngày đã chốt",
    ("sync_job", "status"): "PENDING/RUNNING/SUCCEEDED/FAILED",
    ("sync_job", "batch_id"): "chạy lại không trùng",
    ("staff_payment", "kind"): "SAME_DAY/ADVANCE/MONTHLY",
}
EXTRA_UK = {("customer", "phone")}   # unique một phần (WHERE phone IS NOT NULL)

TYPE_MAP = {
    "bigint": "bigint", "integer": "int", "smallint": "int", "character varying": "varchar", "text": "text",
    "numeric": "numeric", "date": "date", "timestamp with time zone": "timestamptz", "boolean": "boolean",
    "jsonb": "jsonb", "uuid": "uuid",
}


def q(sql):
    # SQL đi qua stdin để tránh lồng dấu nháy khi PSQL_CMD có `su postgres -c "..."`.
    out = subprocess.run(f"{PSQL} -v ON_ERROR_STOP=1 -AtF '|'", shell=True, input=sql,
                         capture_output=True, text=True)
    if out.returncode != 0:
        sys.exit(out.stderr)
    return [line.split("|") for line in out.stdout.splitlines() if line]


def load():
    tables = [r[0] for r in q("select table_name from information_schema.tables where table_schema='public' "
                              "and table_type='BASE TABLE' order by 1")]
    cols = {t: [] for t in tables}
    for t, c, dt, nullable, gen in q("select table_name, column_name, data_type, is_nullable, is_generated "
                                     "from information_schema.columns where table_schema='public' "
                                     "order by table_name, ordinal_position"):
        if t in cols:
            cols[t].append((c, TYPE_MAP.get(dt, dt), nullable == "YES", gen == "ALWAYS"))
    pk = {(t, c) for t, c in q("select tc.table_name, kcu.column_name from information_schema.table_constraints tc "
                               "join information_schema.key_column_usage kcu on kcu.constraint_name=tc.constraint_name "
                               "and kcu.table_schema=tc.table_schema where tc.constraint_type='PRIMARY KEY' "
                               "and tc.table_schema='public'")}
    fk = q("select tc.table_name, kcu.column_name, ccu.table_name from information_schema.table_constraints tc "
           "join information_schema.key_column_usage kcu on kcu.constraint_name=tc.constraint_name "
           "and kcu.table_schema=tc.table_schema join information_schema.constraint_column_usage ccu "
           "on ccu.constraint_name=tc.constraint_name and ccu.table_schema=tc.table_schema "
           "where tc.constraint_type='FOREIGN KEY' and tc.table_schema='public' order by 1,2")
    uk = {(t, c) for t, c in q("select c.relname, a.attname from pg_index i join pg_class c on c.oid=i.indrelid "
                               "join pg_attribute a on a.attrelid=c.oid and a.attnum=i.indkey[0] "
                               "where i.indisunique and i.indnatts=1 and i.indexprs is null and i.indpred is null "
                               "and not i.indisprimary and c.relnamespace='public'::regnamespace")} | EXTRA_UK
    return tables, cols, pk, fk, uk


def entity(t, cols, pk, fkcols, uk):
    lines = [f"    {t} {{"]
    for c, typ, nullable, gen in cols[t]:
        keys = [k for k, on in (("PK", (t, c) in pk), ("FK", (t, c) in fkcols), ("UK", (t, c) in uk)) if on]
        note = COMMENTS.get((t, c), "sinh tự động" if gen else "")
        lines.append(f"        {typ} {c}" + (f" {','.join(keys)}" if keys else "") + (f' "{note}"' if note else ""))
    lines.append("    }")
    return "\n".join(lines)


def relation(t, c, ref, cols, pk, uk):
    nullable = next(n for cc, _, n, _ in cols[t] if cc == c)
    parent = "o|" if nullable else "||"
    one_to_one = (t, c) in uk or (t, c) in pk
    child = "o|" if one_to_one else "o{"
    return f'    {ref} {parent}--{child} {t} : "{c}"'


def main(outdir):
    tables, cols, pk, fk, uk = load()
    fkcols = {(t, c) for t, c, _ in fk}
    os.makedirs(outdir, exist_ok=True)
    # Sơ đồ tổng quan: chỉ quan hệ
    rels = [relation(t, c, r, cols, pk, uk) for t, c, r in fk]
    with open(os.path.join(outdir, "overview.mmd"), "w", encoding="utf-8") as f:
        f.write("erDiagram\n" + "\n".join(rels) + "\n")
    # Từng nhóm: có đầy đủ cột; bảng ngoài nhóm chỉ hiện như điểm nối
    for key, (_, members) in GROUPS.items():
        body = [entity(t, cols, pk, fkcols, uk) for t in members if t in cols]
        grel = [relation(t, c, r, cols, pk, uk) for t, c, r in fk if t in members]
        with open(os.path.join(outdir, f"{key}.mmd"), "w", encoding="utf-8") as f:
            f.write("erDiagram\n" + "\n".join(body) + "\n" + "\n".join(grel) + "\n")
    print(f"Đã sinh {len(GROUPS) + 1} sơ đồ cho {len(tables)} bảng, {len(fk)} quan hệ -> {outdir}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "diagrams")
