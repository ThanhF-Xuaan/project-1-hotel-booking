#!/usr/bin/env python3
"""
Script Linter & Validator kiểm tra cấu trúc hệ sinh thái Agent (.agents/)
Dự án: Hệ thống Quản lý và Đặt phòng Khách sạn Trực tuyến Thông minh
"""

import json
import os
import re
import sys
from pathlib import Path


def check_directory_structure(base_dir: Path) -> bool:
    print("🔍 [1/4] Kiểm tra thư mục cấu trúc .agents/...")
    agents_dir = base_dir / ".agents"
    required_dirs = [
        agents_dir / "rules",
        agents_dir / "skills",
    ]
    has_error = False
    if not agents_dir.exists():
        print("❌ LỖI: Thư mục '.agents' không tồn tại!")
        return False

    for r_dir in required_dirs:
        if not r_dir.exists():
            print(f"❌ LỖI: Thiếu thư mục bắt buộc: {r_dir.relative_to(base_dir)}")
            has_error = True
        else:
            print(f"  ✓ {r_dir.relative_to(base_dir)}")

    return not has_error


def check_placeholders(base_dir: Path) -> bool:
    print("\n🔍 [2/4] Quét các biến Placeholder chưa được thay thế ({{...}})...")
    agents_dir = base_dir / ".agents"
    placeholder_pattern = re.compile(r"\{\{[A-Z0-9_]+\}\}")
    found_placeholders = False

    for file_path in agents_dir.rglob("*"):
        # Bỏ qua các file template (.template.md) vì template được phép chứa placeholder
        if file_path.is_file() and not file_path.name.endswith(".template.md"):
            try:
                content = file_path.read_text(encoding="utf-8")
                matches = placeholder_pattern.findall(content)
                if matches:
                    unique_matches = set(matches)
                    print(
                        f"⚠️  CẢNH BÁO: File '{file_path.relative_to(base_dir)}' còn chứa placeholder: {', '.join(unique_matches)}"
                    )
                    found_placeholders = True
            except Exception as e:
                print(f"❌ Không thể đọc file '{file_path}': {e}")

    if not found_placeholders:
        print("  ✓ Tất cả các file chính thức đã thay thế sạch biến {{...}}")
    return True


def check_mcp_config(base_dir: Path) -> bool:
    print("\n🔍 [3/4] Kiểm tra cú pháp tệp mcpServers.json...")
    mcp_file = base_dir / ".agents" / "mcpServers.json"
    if not mcp_file.exists():
        mcp_file = base_dir / "mcpServers.json"

    if not mcp_file.exists():
        print("⚠️  CẢNH BÁO: Không tìm thấy tệp mcpServers.json!")
        return True

    try:
        with open(mcp_file, "r", encoding="utf-8") as f:
            data = json.load(f)

        if "mcpServers" not in data:
            print("❌ LỖI: 'mcpServers.json' thiếu key gốc 'mcpServers'.")
            return False

        servers = data["mcpServers"]
        print(
            f"  ✓ Tìm thấy {len(servers)} MCP server(s): {', '.join(servers.keys())}"
        )
        return True
    except json.JSONDecodeError as e:
        print(f"❌ LỖI CÚ PHÁP JSON trong file {mcp_file}: {e}")
        return False


def check_markdown_frontmatter(base_dir: Path) -> bool:
    print("\n🔍 [4/4] Kiểm tra YAML Frontmatter trong Rules & Skills...")
    agents_dir = base_dir / ".agents"
    md_files = list(agents_dir.rglob("*.md"))
    has_error = False

    for md_path in md_files:
        if md_path.name.endswith(".template.md"):
            continue

        try:
            content = md_path.read_text(encoding="utf-8")
            if md_path.parent.name == "rules" and not content.startswith("---"):
                print(
                    f"⚠️  CẢNH BÁO: Rule file '{md_path.name}' nên bắt đầu bằng khối YAML Frontmatter ('---')."
                )
        except Exception as e:
            print(f"❌ Lỗi đọc file {md_path}: {e}")

    print("  ✓ Đã kiểm tra xong toàn bộ định dạng Markdown.")
    return not has_error


def main():
    print("==================================================")
    print("🚀 BẮT ĐẦU KIỂM TRA HỆ SINH THÁI ANTIGRAVITY AGENT")
    print("==================================================\n")

    base_dir = Path.cwd()

    s1 = check_directory_structure(base_dir)
    s2 = check_placeholders(base_dir)
    s3 = check_mcp_config(base_dir)
    s4 = check_markdown_frontmatter(base_dir)

    print("\n==================================================")
    if s1 and s2 and s3 and s4:
        print("✅ TẤT CẢ KIỂM TRA ĐỀU THÀNH CÔNG! HỆ THỐNG AGENT SẴN SÀNG.")
        print("==================================================")
        sys.exit(0)
    else:
        print("❌ CÓ LỖI CẤU TRÚC BẮT BUỘC CẦN SỬA TRƯỚC KHI COMMIT!")
        print("==================================================")
        sys.exit(1)


if __name__ == "__main__":
    main()