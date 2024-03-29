package com.intellisrc.term

/**
 * @since 2022/04/12.
 */
class TableMakerAnswers {
    static String getAnswer1() { """
+-----------+-----------+---------------+----------------------+
| Fruit     | QTY       | Price         | Seller               |
|-----------+-----------+---------------+----------------------|
| Apple     | 1000      | 10.00         | some@example.com     |
| Banana    | 2002      | 15.00         | anyone@example.com   |
| Mango     | 400       | 134.10        | dummy200@example.com |
| Kiwi      | 900       | 2350.40       | example@example.com  |
|-----------+-----------+---------------+----------------------|
| Fruits: 4 | Sum: 4302 | Total: 2509.5 |                      |
+-----------+-----------+---------------+----------------------+
""".trim()
    }

    static String getAnswer2() { """
┏━━━━━━━━┳━━━━━━┳━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━┓
┃ Fruit  ┃ QTY  ┃ Price   ┃ Seller               ┃
┣━━━━━━━━╋━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━┫
┃ Apple  ┃ 1000 ┃ 10.00   ┃ some@example.com     ┃
┣━━━━━━━━╋━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━┫
┃ Banana ┃ 2002 ┃ 15.00   ┃ anyone@example.com   ┃
┣━━━━━━━━╋━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━┫
┃ Mango  ┃ 400  ┃ 134.10  ┃ dummy200@example.com ┃
┣━━━━━━━━╋━━━━━━╋━━━━━━━━━╋━━━━━━━━━━━━━━━━━━━━━━┫
┃ Kiwi   ┃ 900  ┃ 2350.40 ┃ example@example.com  ┃
┗━━━━━━━━┻━━━━━━┻━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━┛""".trim() }

    static String getAnswer3() { """
╔═══════════╦═══════════╦═══════════════╦══════════════════════╗
║ Apple     ║ 1000      ║ 10.00         ║ some@example.com     ║
║ Banana    ║ 2002      ║ 15.00         ║ anyone@example.com   ║
║ Mango     ║ 400       ║ 134.10        ║ dummy200@example.com ║
║ Kiwi      ║ 900       ║ 2350.40       ║ example@example.com  ║
╠═══════════╬═══════════╬═══════════════╬══════════════════════╣
║ Fruits: 4 ║ Sum: 4302 ║ Total: 2509.5 ║                      ║
╚═══════════╩═══════════╩═══════════════╩══════════════════════╝""".trim() }

    static String getAnswer4() { """
┌╌╌╌╌╌╌╌╌┬╌╌╌╌╌╌┬╌╌╌╌╌╌╌╌╌┬╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┐
┊ Apple  ┊ 1000 ┊ 10.00   ┊ some@example.com     ┊
├╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┤
┊ Banana ┊ 2002 ┊ 15.00   ┊ anyone@example.com   ┊
├╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┤
┊ Mango  ┊ 400  ┊ 134.10  ┊ dummy200@example.com ┊
├╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌┼╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┤
┊ Kiwi   ┊ 900  ┊ 2350.40 ┊ example@example.com  ┊
└╌╌╌╌╌╌╌╌┴╌╌╌╌╌╌┴╌╌╌╌╌╌╌╌╌┴╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌┘""".trim() }

    static String getAnswer5() { """
╔═══════════╤═══════════╤═══════════════╤══════════════════════╗
║ Fruit     │ QTY       │ Price         │ Seller               ║
╟───────────┼───────────┼───────────────┼──────────────────────╢
║ Apple     │ 1000      │ 10.00         │ some@example.com     ║
╟───────────┼───────────┼───────────────┼──────────────────────╢
║ Banana    │ 2002      │ 15.00         │ anyone@example.com   ║
╟───────────┼───────────┼───────────────┼──────────────────────╢
║ Mango     │ 400       │ 134.10        │ dummy200@example.com ║
╟───────────┼───────────┼───────────────┼──────────────────────╢
║ Kiwi      │ 900       │ 2350.40       │ example@example.com  ║
╟───────────┼───────────┼───────────────┼──────────────────────╢
║ Fruits: 4 │ Sum: 4302 │ Total: 2509.5 │                      ║
╚═══════════╧═══════════╧═══════════════╧══════════════════════╝""".trim() }
    static String getAnswer6() { """
╔════════╤══════╤═════════╤══════════════════════╗
║ Fruit  │ QTY  │ Price   │ Seller               ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Apple  │ 1000 │ 10.00   │ some@example.com     ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Banana │ 2002 │ 15.00   │ anyone@example.com   ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Mango  │ 400  │ 134.10  │ dummy200@example.com ║
╟────────┼──────┼─────────┼──────────────────────╢
║ Kiwi   │ 900  │ 2350.40 │ example@example.com  ║
╚════════╧══════╧═════════╧══════════════════════╝""".trim() }
    static String getAnswer7() { """
┌───────────────────┬────────────────────────────────┬─────┐
│ Name              │ Email                          │ Age │
├───────────────────┼────────────────────────────────┼─────┤
│ Joshep Patrishius │ jp@example.com                 │ 41  │
│ Marilin Watson    │ marilin-watson1023@example.com │ 19  │
│ Raphael Kawami    │ kawami-rapha@example.com       │ 33  │
│ Zoe Mendoza       │ you-know-who@example.com       │ 54  │
├───────────────────┴────────────────────────────────┴─────┤
│ All names here are fictitious                            │
└──────────────────────────────────────────────────────────┘""".trim() }
    static String getAnswer8() { """
┌───────────────────┬────────────┬──────────────┐
│ Name              │ User Emai… │         Cost │
├───────────────────┼────────────┼──────────────┤
│ Joshep Patrishius │ jp@exampl… │      \$ 41.00 │
│ Marilin Watson    │ marilin-w… │    \$ 1339.00 │
│ Raphael Kawami    │ kawami-ra… │    \$ 3893.00 │
│ Zoe Mendoza       │ you-know-… │ \$ 1999254.00 │
├───────────────────┴────────────┴──────────────┤
│ All names here are fictitious                 │
└───────────────────────────────────────────────┘""".trim() }
    static String getAnswer9() { """
+--------+------+---------+----------------------+
| Fruit  | QTY  | Price   | Seller               |
|--------+------+---------+----------------------|
| Apple  | 1000 | 10.00   | some@example.com     |
|--------+------+---------+----------------------|
| Banana | 2002 | 15.00   | anyone@example.com   |
|--------+------+---------+----------------------|
| Mango  | 400  | 134.10  | dummy200@example.com |
|--------+------+---------+----------------------|
| Kiwi   | 900  | 2350.40 | example@example.com  |
|--------+------+---------+----------------------|
| Fruits: 4                                      |
+------------------------------------------------+""".trim() }
    static String getAnswer10() { """
+--------+------+---------+--------------+
| Fruit  | QTY  | Price   |       Seller |
|--------+------+---------+--------------|
| Apple  | 1000 | 10.00   | …example.com |
|--------+------+---------+--------------|
| Banana | 2002 | 15.00   | …example.com |
|--------+------+---------+--------------|
| Mango  | 400  | 134.10  | …example.com |
|--------+------+---------+--------------|
| Kiwi   | 900  | 2350.40 | …example.com |
|--------+------+---------+--------------|
| Fruits: 4                              |
+----------------------------------------+""".trim() }
    static String getAnswer11() { """
+--------+------+---------+--------------------------------+
| Fruit  | QTY  | Price   |             Seller             |
|--------+------+---------+--------------------------------|
| Apple  | 1000 | 10.00   |        some@example.com        |
|--------+------+---------+--------------------------------|
| Banana | 2002 | 15.00   |         a@example.com          |
|--------+------+---------+--------------------------------|
| Mango  | 400  | 134.10  | very_long_dummy200@example.com |
|--------+------+---------+--------------------------------|
| Kiwi   | 900  | 2350.40 |      example@example.com       |
|--------+------+---------+--------------------------------|
| Fruits: 4                                                |
+----------------------------------------------------------+""".trim() }
    static String getAnswer12() { """
+--------+------+---------+-----------------+
| Fruit  | QTY  | Price   |     Seller      |
|--------+------+---------+-----------------|
| Orange | 800  | 1.00    | ben@example.com |
|--------+------+---------+-----------------|
| Apple  | 1000 | 10.00   | some@example.c… |
|--------+------+---------+-----------------|
| Banana | 2002 | 15.00   |  a@example.com  |
|--------+------+---------+-----------------|
| Mango  | 400  | 134.10  | …g_dummy200@ex… |
|--------+------+---------+-----------------|
| Kiwi   | 900  | 2350.40 | …mple@example.… |
|--------+------+---------+-----------------|
| Fruits: 4                                 |
+-------------------------------------------+""".trim() }
    static String getAnswer13() { """
+--------+------+------------+--------------------------------+
| Fruit  | QTY  | Price      | Seller                         |
|--------+------+------------+--------------------------------|
| Apple  | 1000 | 101        | some@example.com               |
|--------+------+------------+--------------------------------|
| Banana | 2002 | 150        | a@example.com                  |
|--------+------+------------+--------------------------------|
| Mango  | 400  | 134        | very_long_dummy200@example.com |
|--------+------+------------+--------------------------------|
| Kiwi   | 900  | 235        | example@example.com            |
|--------+------+------------+--------------------------------|
| Fruits: 4                                                   |
+-------------------------------------------------------------+""".trim() }
    static String getAnswer14() { """
+--------+------+------+--------------------------------+
| Fruit  | QTY  | Pri… | Seller                         |
|--------+------+------+--------------------------------|
| Apple  | 1000 | 1    | some@example.com               |
|--------+------+------+--------------------------------|
| Banana | 2002 | 150  | a@example.com                  |
|--------+------+------+--------------------------------|
| Mango  | 400  | 1344 | very_long_dummy200@example.com |
|--------+------+------+--------------------------------|
| Kiwi   | 900  | 23   | example@example.com            |
|--------+------+------+--------------------------------|
| Fruits: 4                                             |
+-------------------------------------------------------+""".trim() }
    static String getAnswer15() { """
+--------+------+--------------------------------+
| Fruit  | QTY  | Seller                         |
|--------+------+--------------------------------|
| Apple  | 1000 | some@example.com               |
|--------+------+--------------------------------|
| Banana | 2002 | a@example.com                  |
|--------+------+--------------------------------|
| Mango  | 400  | very_long_dummy200@example.com |
|--------+------+--------------------------------|
| Kiwi   | 900  | example@example.com            |
|--------+------+--------------------------------|
| Fruits: 4                                      |
+------------------------------------------------+""".trim() }
    static String getAnswer16() { """
+-------+------+------+------+
| Fruit | QTY  | Pri… | Sel… |
|-------+------+------+------|
| Apple | 1000 | 1234 |      |
|-------+------+------+------|
| Bana… | 2002 | 522… |      |
|-------+------+------+------|
| Mango | 400  | 164… | ver… |
|-------+------+------+------|
| Kiwi  | 500  | 918… |      |
|-------+------+------+------|
| Oran… | 800  | 456… |      |
|-------+------+------+------|
| Papa… | 900  | 237… |      |
|-------+------+------+------|
| Yuzu  | 100  | 22   |      |
|-------+------+------+------|
| Stra… | 300  | 556  |      |
|-------+------+------+------|
| Wate… | 7900 | 1    |      |
|-------+------+------+------|
| Melon | 700  | 678  |      |
|-------+------+------+------|
| Lemon | 900  | 553  |      |
|-------+------+------+------|
| Guava | 2000 | 126… |      |
|-------+------+------+------|
| Grap… | 700  | 21   |      |
|-------+------+------+------|
| Lime  | 5400 | 235  |      |
|-------+------+------+------|
| Fruits: 4                  |
+----------------------------+""".trim() }
    static String getAnswer17() { """
+-----------+------+--------+--------------------------------+
| 果物      | QTY  | 金額   |             メール             |
|-----------+------+--------+--------------------------------|
| りんご    | 1000 | 10.00  |        some@example.com        |
|-----------+------+--------+--------------------------------|
| バナナ    | 2002 | 15.00  |         a@example.com          |
|-----------+------+--------+--------------------------------|
| マンゴー   | 400  | 134.10 | very_long_dummy200@example.com |
|-----------+------+--------+--------------------------------|
| キウイ    | 900  | 2350   |      example@example.com       |
|-----------+------+--------+--------------------------------|
| ﾄﾓﾛｺｼ     | 100  | 1000   |      example@example.com       |
|-----------+------+--------+--------------------------------|
| ௵౷ആ | 200  | 2000   |      example@example.com       |
|-----------+------+--------+--------------------------------|
| ⛰⛱⛲⛳⛴     | 300  | 3000   |      example@example.com       |
|-----------+------+--------+--------------------------------|
| 合計: 4                                                    |
+------------------------------------------------------------+""".trim() }
    static String getAnswer18() { """
+------------+-------------------+
| name       | Samantha Wigs     |
|------------+-------------------|
| age        | 25                |
|------------+-------------------|
| country    | New Zealand       |
|------------+-------------------|
| occupation | Teacher           |
|------------+-------------------|
| email      | sam25@teachers.nz |
|------------+-------------------|
| phone      | 098-8712-9378     |
+------------+-------------------+
""".trim() }
    static String getAnswer19() { """
+---------------+-----+-------------+------------+-------------------+---------------+
| name          | age | country     | occupation | email             | phone         |
|---------------+-----+-------------+------------+-------------------+---------------|
| Samantha Wigs | 25  | New Zealand | Teacher    | sam25@teachers.nz | 098-8712-9378 |
+---------------+-----+-------------+------------+-------------------+---------------+
""".trim() }
    static String getAnswer20() { """""".trim() }

}
