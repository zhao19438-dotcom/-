-- 仅统一已知历史公告版块和对应普通帖子；不删除数据，可重复执行。
UPDATE t_board SET bizType = 'announcement'
WHERE name = '校园公告' AND bizType = 'forum';

UPDATE t_article a JOIN t_board b ON b.id = a.boardId
SET a.bizType = 'announcement'
WHERE b.bizType = 'announcement' AND a.bizType = 'forum';
