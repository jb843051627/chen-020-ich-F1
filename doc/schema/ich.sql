-- ich 非物质文化遗产代表性项目与代表性传承人管理服务 -- schema (chen-020)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_020

CREATE TABLE IF NOT EXISTS t_ich_cycle_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '履约考核到期单',
  due_at datetime DEFAULT NULL COMMENT '该出手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '可提前几日开口',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对名录项目或传承人记要',
  status int DEFAULT NULL COMMENT '条目情形 0候办 1已办妥 2催不动',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履约考核到期单';

CREATE TABLE IF NOT EXISTS t_ich_grant_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '工坊认定核准单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（县文旅/市文旅/省文旅）',
  sign_mode int DEFAULT NULL COMMENT '同档并签办法 0一笔即可 1两笔点齐',
  need_count int DEFAULT NULL COMMENT '本档应落名数',
  sign_count int DEFAULT NULL COMMENT '本档已落名数',
  status int DEFAULT NULL COMMENT '核准情形 0在核 1已认下 2已挪回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工坊认定核准单';

CREATE TABLE IF NOT EXISTS t_ich_image_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '技艺影像卷卷号',
  node_no int DEFAULT NULL COMMENT '本卷版次序',
  site_id int DEFAULT NULL COMMENT '所属名录项目',
  site_no varchar(64) DEFAULT NULL COMMENT '所属项目代号',
  plan_qty decimal(12,2) DEFAULT NULL COMMENT '应归影像件数',
  real_qty decimal(12,2) DEFAULT NULL COMMENT '实归影像件数',
  dev_rate decimal(12,2) DEFAULT NULL COMMENT '两数折出的偏差率',
  content varchar(255) DEFAULT NULL COMMENT '随卷交来的门类要件',
  status int DEFAULT NULL COMMENT '建卷进展 0待核对 1已核对 2已冻住',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='技艺影像卷立卷单';

CREATE TABLE IF NOT EXISTS t_ich_item_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '名录项目申报单',
  stage int DEFAULT NULL COMMENT '当前格次 0..3（核验/评议/公示/列入）',
  status int DEFAULT NULL COMMENT '申领会落 0未起 1在办 2已收口',
  content varchar(255) DEFAULT NULL COMMENT '一格一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录项目申报单';

CREATE TABLE IF NOT EXISTS t_ich_pre_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '准入门槛线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '起分数值',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '过线数值',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '优线数值',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '让位顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在场 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报准入门槛线';

CREATE TABLE IF NOT EXISTS t_ich_project (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '名录项目代号',
  site_name varchar(128) DEFAULT NULL COMMENT '项目名称全称',
  site_type varchar(32) DEFAULT NULL COMMENT '门类',
  road_name varchar(128) DEFAULT NULL COMMENT '认定到哪一级(县—市—省)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已注销',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='名录项目底册';

CREATE TABLE IF NOT EXISTS t_ich_report_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '履职情况报送批号',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的传承人代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行进账场数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本行授徒时数',
  status int DEFAULT NULL COMMENT '行落地情形 0未收 1已收下 2已退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='履职情况报送核收行';

-- 初始底册数据：两条项目底册，一条在册、一条已注销（注销后不得再挂接新立卷）。
-- t_ich_project 两条种子底册：id=0 在册、id=1 已注销；项目代号与门类按门类口径给值。
INSERT IGNORE INTO t_ich_project (id, site_no, site_name, site_type, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'XM00', '澜川县·苗绣（银苗绣片技艺，在册）', '传统技艺', '澜川县—河湾区—长街上', 0, 0, 'seed', NOW()),
       (1, 'XM01', '澜川县·侗族大歌（歌师已故项目，已注销）', '传统音乐', '澜川县—河湾区—滨河北', 1, 0, 'seed', NOW());

