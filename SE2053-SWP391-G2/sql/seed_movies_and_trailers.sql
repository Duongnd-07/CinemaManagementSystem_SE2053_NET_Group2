-- ==============================================================================
-- Script seed dữ liệu phim đầy đủ: Poster HD, Trailer YouTube chính thức, 
-- Thể loại, Vòng đời phim và Suất chiếu mẫu
-- Chạy an toàn nhiều lần (sử dụng MERGE / IF NOT EXISTS)
-- ==============================================================================

USE CinemaDB;
GO

SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

-- 1. Bổ sung các thể loại phim chuẩn nếu chưa có
IF NOT EXISTS (SELECT 1 FROM Genre WHERE GenreName = 'Animation') INSERT INTO Genre (GenreName) VALUES ('Animation');
IF NOT EXISTS (SELECT 1 FROM Genre WHERE GenreName = 'Adventure') INSERT INTO Genre (GenreName) VALUES ('Adventure');
IF NOT EXISTS (SELECT 1 FROM Genre WHERE GenreName = 'Romance')   INSERT INTO Genre (GenreName) VALUES ('Romance');
IF NOT EXISTS (SELECT 1 FROM Genre WHERE GenreName = 'Family')    INSERT INTO Genre (GenreName) VALUES ('Family');
IF NOT EXISTS (SELECT 1 FROM Genre WHERE GenreName = 'Fantasy')   INSERT INTO Genre (GenreName) VALUES ('Fantasy');
GO

-- 2. Cập nhật và thêm mới danh sách phim
-- Movie 1: Lật Mặt 7: Một Điều Ước
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 1)
BEGIN
    UPDATE Movie SET
        Title = N'Lật Mặt 7: Một Điều Ước',
        Synopsis = N'Câu chuyện xoay quanh bà Hai 73 tuổi sống một mình tại ngôi nhà đơn sơ giữa đồi hoa bất tử ở huyện Lạc Dương (Lâm Đồng). Bà có 5 người con nhưng mỗi người một nơi, ai cũng có nỗi khổ và gánh nặng riêng. Một biến cố xảy ra khiến những người con phải cùng nhau tìm cách chăm sóc mẹ già, qua đó gợi lên tình mẫu tử thiêng liêng và sâu sắc.',
        Director = N'Lý Hải',
        Cast = N'Đinh Y Nhung, Quách Ngọc Tuyên, Trâm Anh, Thanh Thức, Tín Nguyễn',
        DurationMinutes = 138,
        AgeRating = 'P',
        ReleaseDate = '2024-04-26',
        EndDate = '2026-12-31',
        Country = N'Việt Nam',
        [Language] = N'Tiếng Việt',
        Distributor = N'Lý Hải Production',
        PosterURL = 'https://m.media-amazon.com/images/M/MV5BMjA1OTU5NTEtNDZlYS00NDM0LWExNGEtNmY2OTU3ODMzZWE0XkEyXkFqcGc@._V1_.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=d_kHIdB5kPQ',
        Status = 'Now Showing',
        Formats = '2D'
    WHERE MovieID = 1;
END

-- Movie 2: Dune: Hành Tinh Cát - Phần Hai
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 2)
BEGIN
    UPDATE Movie SET
        Title = N'Dune: Hành Tinh Cát - Phần Hai',
        Synopsis = N'Hành trình thần thoại của Paul Atreides khi anh hợp nhất với Chani và người Fremen trên con đường báo thù chống lại những kẻ âm mưu tiêu diệt gia tộc mình. Đứng trước sự lựa chọn giữa tình yêu của đời mình và số phận của vũ trụ, Paul nỗ lực ngăn chặn một tương lai đen tối khủng khiếp mà chỉ mình anh có thể thấy trước.',
        Director = N'Denis Villeneuve',
        Cast = N'Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem, Austin Butler, Florence Pugh',
        DurationMinutes = 166,
        AgeRating = 'C13',
        ReleaseDate = '2024-03-01',
        EndDate = '2026-12-31',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Warner Bros. Pictures',
        PosterURL = 'https://m.media-amazon.com/images/M/MV5BN2QyZGUgkOGMtYjhiYS00NDIzLWFkNDYtMzFjNGEwYTE3ODQ4XkEyXkFqcGc@._V1_.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=Way9Dexny3w',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 2;
END

-- Movie 3: Marvel Studios' Avengers: Endgame
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 3)
BEGIN
    UPDATE Movie SET
        Title = N'Marvel Studios'' Avengers: Endgame',
        Synopsis = N'Sau sự kiện tàn khốc trong Avengers: Infinity War, vũ trụ bị hủy hoại một nửa dưới tay bạo chúa Thanos. Với sự trợ giúp của các đồng minh còn sống sót, các thành viên biệt đội Avengers một lần nữa tập hợp để thực hiện kế hoạch du hành thời gian, đảo ngược hành động của Thanos và khôi phục lại trật tự cho toàn vũ trụ.',
        Director = N'Anthony Russo, Joe Russo',
        Cast = N'Robert Downey Jr., Chris Evans, Mark Ruffalo, Chris Hemsworth, Scarlett Johansson, Jeremy Renner',
        DurationMinutes = 181,
        AgeRating = 'C13',
        ReleaseDate = '2019-04-26',
        EndDate = '2026-12-31',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Walt Disney Studios',
        PosterURL = 'https://m.media-amazon.com/images/M/MV5BMTc5MDE2ODcwNV5BMl5BanBnXkFtZTgwMzI2NzQ2NzM@._V1_.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=TcMBFSGVi1c',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 3;
END

-- Movie 4: Doraemon: Nobita và Bản Giao Hưởng Địa Cầu
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 4)
BEGIN
    UPDATE Movie SET
        Title = N'Doraemon: Nobita và Bản Giao Hưởng Địa Cầu',
        Synopsis = N'Doraemon và nhóm bạn bước vào cuộc phiêu lưu âm nhạc hoành tráng ngoài vũ trụ để giải cứu Trái Đất khỏi một sinh vật bí ẩn đang đe dọa nuốt chửng năng lượng âm nhạc của thế giới. Bằng sức mạnh của tình bạn và những giai điệu kỳ diệu, cả nhóm cùng nhau hòa tấu bản giao hưởng cứu lấy địa cầu.',
        Director = N'Imai Kazuaki',
        Cast = N'Mizuta Wasabi, Ohara Megumi, Kakazu Yumi, Kimura Subaru, Seki Tomokazu',
        DurationMinutes = 115,
        AgeRating = 'P',
        ReleaseDate = '2024-05-24',
        EndDate = '2026-12-31',
        Country = N'Nhật Bản',
        [Language] = N'Tiếng Nhật',
        Distributor = N'Toho / CJ HK Entertainment',
        PosterURL = 'https://m.media-amazon.com/images/M/MV5BNmE0YjllMjctYTFmYS00M2NkLTk5YmQtYjRhMDlhOWM3YmE5XkEyXkFqcGc@._V1_.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=2Ox6aYmhxvw',
        Status = 'Now Showing',
        Formats = '2D, 3D'
    WHERE MovieID = 4;
END

-- Thêm các phim bom tấn mới (Now Showing & Coming Soon)
DECLARE @NewMovies TABLE (
    Title NVARCHAR(200),
    Synopsis NVARCHAR(MAX),
    Director NVARCHAR(100),
    Cast NVARCHAR(500),
    DurationMinutes INT,
    AgeRating VARCHAR(10),
    ReleaseDate DATE,
    EndDate DATE,
    Country NVARCHAR(100),
    [Language] NVARCHAR(100),
    Distributor NVARCHAR(200),
    PosterURL VARCHAR(500),
    TrailerURL VARCHAR(500),
    Status VARCHAR(20),
    Formats VARCHAR(50)
);

INSERT INTO @NewMovies VALUES
(
    N'Deadpool & Wolverine',
    N'Tổ chức Phương sai Thời gian (TVA) lôi kéo Wade Wilson (Deadpool) từ cuộc sống yên bình vào một sứ mệnh mới. Cùng với một biến thể Wolverine chán chường, bộ đôi bất đắc dĩ phải hợp lực để cứu lấy dòng thời gian của mình trước một mối hiểm họa diệt vong vô cùng tàn khốc.',
    N'Shawn Levy',
    N'Ryan Reynolds, Hugh Jackman, Emma Corrin, Matthew Macfadyen, Dafne Keen',
    128,
    'C18',
    '2024-07-26',
    '2026-12-31',
    N'Mỹ',
    N'Tiếng Anh',
    N'Marvel Studios',
    'https://m.media-amazon.com/images/M/MV5BNzRiMjg0MzUtNTQ1Eg00OGNkLTllMmUtMTk1NzU1ZTBhZGY5XkEyXkFqcGc@._V1_.jpg',
    'https://www.youtube.com/watch?v=73_1biulkYk',
    'Now Showing',
    '2D, 3D, IMAX'
),
(
    N'Godzilla x Kong: The New Empire',
    N'Trận chiến bùng nổ tiếp theo của Monsterverse khi Kong toàn năng và Godzilla đáng sợ phải bắt tay nhau đối đầu với một mối đe dọa khổng lồ chưa từng được khám phá, ẩn sâu bên trong Trái Đất rỗng, đe dọa sự tồn vong của loài người cũng như toàn bộ các Titan.',
    N'Adam Wingard',
    N'Rebecca Hall, Brian Tyree Henry, Dan Stevens, Kaylee Hottle, Alex Ferns',
    115,
    'C13',
    '2024-03-29',
    '2026-12-31',
    N'Mỹ',
    N'Tiếng Anh',
    N'Warner Bros. Pictures',
    'https://m.media-amazon.com/images/M/MV5BMjA1OTY0MTY1Nl5BMl5BanBnXkFtZTgwNTU0NzkwMzI@._V1_.jpg',
    'https://www.youtube.com/watch?v=lV1OOlGwExg',
    'Now Showing',
    '2D, 3D, IMAX'
),
(
    N'Kung Fu Panda 4',
    N'Po được chỉ định trở thành Thủ lĩnh Tinh thần của Thung lũng Bình Yên và cần tìm một Thần Long Đại Hiệp mới kế vị. Trong lúc đó, một phù thủy biến hình gian xảo tên Tắc Kè Hoa xuất hiện, nhăm nhe chiếm đoạt cây gậy phép của Po để triệu hồi lại linh hồn của tất cả những kẻ phản diện mà Po từng đánh bại.',
    N'Mike Mitchell',
    N'Jack Black, Awkwafina, Viola Davis, Dustin Hoffman, Bryan Cranston',
    94,
    'P',
    '2024-03-08',
    '2026-12-31',
    N'Mỹ',
    N'Tiếng Anh',
    N'Universal Pictures',
    'https://m.media-amazon.com/images/M/MV5BYTJlNmI4NTUtZDJhNS00YzBlLWEyODItZTk3YTA1NmE3ZTY2XkEyXkFqcGc@._V1_.jpg',
    'https://www.youtube.com/watch?v=_inKs4eeHiI',
    'Now Showing',
    '2D, 3D'
),
(
    N'Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)',
    N'Bộ phim quay trở lại tâm trí của cô bé Riley lúc này đã bước vào tuổi dậy thì đầy biến động. Trụ sở đầu não bất ngờ đón nhận một cuộc cải tạo lớn khi các cảm xúc mới bất thình lình ập đến: Lo Âu (Anxiety), Ganh Tị (Envy), Chán Nản (Ennui) và Xấu Hổ (Embarrassment), đẩy nhóm cảm xúc cũ vào một hành trình đầy cam go.',
    N'Kelsey Mann',
    N'Amy Poehler, Maya Hawke, Kensington Tallman, Liza Lapira, Tony Hale',
    96,
    'P',
    '2024-06-14',
    '2026-12-31',
    N'Mỹ',
    N'Tiếng Anh',
    N'Walt Disney Studios',
    'https://m.media-amazon.com/images/M/MV5BYWY6ZWVhNzYtM2VlZi00ZGRmLWFkYTYtYzg0MTM2NzFlOTBhXkEyXkFqcGc@._V1_.jpg',
    'https://www.youtube.com/watch?v=LEjhY15eCx0',
    'Now Showing',
    '2D, 3D'
),
(
    N'Mai',
    N'Mai là một người phụ nữ gần 30 tuổi làm nghề mát-xa trị liệu, luôn chịu nhiều định kiến xã hội và mang trong mình những tổn thương quá khứ khó giãi bày. Cuộc đời cô bắt đầu rẽ hướng khi Dương - một chàng trai đào hoa, trẻ tuổi sống cùng khu chung cư - kiên trì theo đuổi và tìm cách sưởi ấm trái tim cô.',
    N'Trấn Thành',
    N'Phương Anh Đào, Tuấn Trần, Trấn Thành, Hồng Đào, Uyển Ân',
    131,
    'C18',
    '2024-02-10',
    '2026-12-31',
    N'Việt Nam',
    N'Tiếng Việt',
    N'CJ HK Entertainment',
    'https://m.media-amazon.com/images/M/MV5BNTI4YmM2MjItYmFhYi00ZTQ3LWE0NzQtNTRhNmY4NmViNGMyXkEyXkFqcGc@._V1_.jpg',
    'https://www.youtube.com/watch?v=tfgIiqXq7c8',
    'Now Showing',
    '2D'
),
(
    N'Moana 2 (Hành Trình Của Moana 2)',
    N'Ba năm sau chuyến hành trình đầu tiên, Moana nhận được một lời kêu gọi bất ngờ từ tổ tiên định hướng đại dương. Cùng với Maui và một thủy thủ đoàn gồm những người đi biển kỳ quặc, Moana phải dấn thân vào vùng biển xa xôi nguy hiểm của châu Đại Dương để phá vỡ lời nguyền cổ xưa.',
    N'David G. Derrick Jr.',
    N'Auliʻi Cravalho, Dwayne Johnson, Alan Tudyk, Temuera Morrison, Nicole Scherzinger',
    100,
    'P',
    '2026-11-27',
    '2027-02-28',
    N'Mỹ',
    N'Tiếng Anh',
    N'Walt Disney Studios',
    'https://m.media-amazon.com/images/M/MV5BYTNmMzU1NzktMGU2NS00NjJkLWJmNjEtYmRmYjNlODFiZTk4XkEyXkFqcGc@._V1_.jpg',
    'https://www.youtube.com/watch?v=hDZ7y8RP5HE',
    'Coming Soon',
    '2D, 3D'
),
(
    N'Mufasa: The Lion King',
    N'Khám phá câu chuyện thời niên thiếu của Mufasa - từ một chú sư tử con mồ côi lạc lõng, đơn độc cho đến khi gặp gỡ người thừa kế hoàng gia Taka và cùng nhau dấn thân vào hành trình vĩ đại tôi luyện nên vị Vua Sư Tử huyền thoại của Vùng Đất Kiêu Hãnh.',
    N'Barry Jenkins',
    N'Aaron Pierre, Kelvin Harrison Jr., Seth Rogen, Billy Eichner, Donald Glover',
    118,
    'P',
    '2026-12-20',
    '2027-02-28',
    N'Mỹ',
    N'Tiếng Anh',
    N'Walt Disney Studios',
    'https://m.media-amazon.com/images/M/MV5BN2E2YWY3OTYtNmI0Zi00ZTVjLWJjNWUtYjViMWVjZmNlY2VhXkEyXkFqcGc@._V1_.jpg',
    'https://www.youtube.com/watch?v=o17MF9vnabg',
    'Coming Soon',
    '2D, 3D, IMAX'
);

-- Thêm các phim mới vào bảng Movie nếu chưa có (theo Tiêu đề)
INSERT INTO Movie (Title, Synopsis, Director, Cast, DurationMinutes, AgeRating, ReleaseDate, EndDate, Country, [Language], Distributor, PosterURL, TrailerURL, Status, Formats)
SELECT src.Title, src.Synopsis, src.Director, src.Cast, src.DurationMinutes, src.AgeRating, src.ReleaseDate, src.EndDate, src.Country, src.[Language], src.Distributor, src.PosterURL, src.TrailerURL, src.Status, src.Formats
FROM @NewMovies src
WHERE NOT EXISTS (SELECT 1 FROM Movie m WHERE m.Title = src.Title);

-- 3. Gán thể loại cho các phim
-- Helper map thể loại
DECLARE @GenreAction INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Action');
DECLARE @GenreSciFi INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Sci-Fi');
DECLARE @GenreDrama INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Drama');
DECLARE @GenreComedy INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Comedy');
DECLARE @GenreAnimation INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Animation');
DECLARE @GenreAdventure INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Adventure');
DECLARE @GenreRomance INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Romance');
DECLARE @GenreFamily INT = (SELECT GenreID FROM Genre WHERE GenreName = 'Family');

-- Gán thể loại cho từng phim
DECLARE @MId INT;

-- Deadpool & Wolverine (Action, Comedy, Sci-Fi)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Deadpool & Wolverine';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAction) INSERT INTO Movie_Genre VALUES (@MId, @GenreAction);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreComedy) INSERT INTO Movie_Genre VALUES (@MId, @GenreComedy);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreSciFi) INSERT INTO Movie_Genre VALUES (@MId, @GenreSciFi);
END

-- Godzilla x Kong (Action, Sci-Fi, Adventure)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Godzilla x Kong: The New Empire';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAction) INSERT INTO Movie_Genre VALUES (@MId, @GenreAction);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreSciFi) INSERT INTO Movie_Genre VALUES (@MId, @GenreSciFi);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAdventure) INSERT INTO Movie_Genre VALUES (@MId, @GenreAdventure);
END

-- Kung Fu Panda 4 (Animation, Action, Comedy, Family)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Kung Fu Panda 4';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAnimation) INSERT INTO Movie_Genre VALUES (@MId, @GenreAnimation);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAction) INSERT INTO Movie_Genre VALUES (@MId, @GenreAction);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreComedy) INSERT INTO Movie_Genre VALUES (@MId, @GenreComedy);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreFamily) INSERT INTO Movie_Genre VALUES (@MId, @GenreFamily);
END

-- Inside Out 2 (Animation, Comedy, Drama, Family)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAnimation) INSERT INTO Movie_Genre VALUES (@MId, @GenreAnimation);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreComedy) INSERT INTO Movie_Genre VALUES (@MId, @GenreComedy);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreFamily) INSERT INTO Movie_Genre VALUES (@MId, @GenreFamily);
END

-- Mai (Drama, Romance)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Mai';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreDrama) INSERT INTO Movie_Genre VALUES (@MId, @GenreDrama);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreRomance) INSERT INTO Movie_Genre VALUES (@MId, @GenreRomance);
END

-- Moana 2 (Animation, Adventure, Comedy)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Moana 2 (Hành Trình Của Moana 2)';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAnimation) INSERT INTO Movie_Genre VALUES (@MId, @GenreAnimation);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAdventure) INSERT INTO Movie_Genre VALUES (@MId, @GenreAdventure);
END

-- Mufasa (Animation, Adventure, Drama)
SELECT @MId = MovieID FROM Movie WHERE Title = N'Mufasa: The Lion King';
IF @MId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAnimation) INSERT INTO Movie_Genre VALUES (@MId, @GenreAnimation);
    IF NOT EXISTS (SELECT 1 FROM Movie_Genre WHERE MovieID = @MId AND GenreID = @GenreAdventure) INSERT INTO Movie_Genre VALUES (@MId, @GenreAdventure);
END

-- 4. Thêm các suất chiếu mẫu cho các phim mới
DECLARE @ShowtimeData TABLE (
    MovieTitle NVARCHAR(200),
    RoomID INT,
    StartTime DATETIME,
    Status VARCHAR(20),
    TranslationType NVARCHAR(20)
);

INSERT INTO @ShowtimeData VALUES
(N'Deadpool & Wolverine', 4, '2026-10-24 19:00:00', 'Active', N'Phụ đề'),
(N'Mai', 4, '2026-10-24 21:45:00', 'Active', N'Phụ đề'),
(N'Lật Mặt 7: Một Điều Ước', 1, '2026-10-25 13:30:00', 'Active', N'Phụ đề'),
(N'Dune: Hành Tinh Cát - Phần Hai', 2, '2026-10-25 14:00:00', 'Active', N'Phụ đề'),
(N'Godzilla x Kong: The New Empire', 2, '2026-10-25 18:00:00', 'Active', N'Phụ đề'),
(N'Kung Fu Panda 4', 4, '2026-10-25 09:30:00', 'Active', N'Lồng tiếng'),
(N'Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)', 4, '2026-10-25 14:30:00', 'Active', N'Lồng tiếng'),
(N'Deadpool & Wolverine', 4, '2026-10-25 19:30:00', 'Active', N'Phụ đề'),
(N'Kung Fu Panda 4', 1, '2026-10-26 09:30:00', 'Active', N'Lồng tiếng'),
(N'Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)', 1, '2026-10-26 14:00:00', 'Active', N'Phụ đề'),
(N'Deadpool & Wolverine', 1, '2026-10-26 19:00:00', 'Active', N'Phụ đề'),
(N'Lật Mặt 7: Một Điều Ước', 2, '2026-10-26 10:00:00', 'Active', N'Phụ đề'),
(N'Godzilla x Kong: The New Empire', 2, '2026-10-26 14:30:00', 'Active', N'Lồng tiếng'),
(N'Dune: Hành Tinh Cát - Phần Hai', 2, '2026-10-26 19:30:00', 'Active', N'Phụ đề'),
(N'Doraemon: Nobita và Bản Giao Hưởng Địa Cầu', 4, '2026-10-26 10:00:00', 'Active', N'Lồng tiếng'),
(N'Mai', 4, '2026-10-26 14:00:00', 'Active', N'Phụ đề'),
(N'Marvel Studios'' Avengers: Endgame', 4, '2026-10-26 19:30:00', 'Active', N'Phụ đề');

INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, Status, TranslationType)
SELECT 
    m.MovieID, 
    s.RoomID, 
    s.StartTime, 
    DATEADD(MINUTE, m.DurationMinutes, s.StartTime) AS EndTime,
    CASE WHEN s.RoomID = 2 THEN 110000 ELSE 85000 END AS BasePrice,
    s.Status, 
    s.TranslationType
FROM @ShowtimeData s
JOIN Movie m ON m.Title = s.MovieTitle
WHERE NOT EXISTS (
    SELECT 1 FROM Showtime existing 
    WHERE existing.RoomID = s.RoomID AND existing.StartTime = s.StartTime
);

PRINT N'Đã hoàn tất nạp và cập nhật danh sách phim chất lượng cao kèm Poster & Trailer YouTube và Suất chiếu!';
GO
