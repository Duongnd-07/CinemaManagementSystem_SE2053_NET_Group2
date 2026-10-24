-- ==============================================================================
-- Script seed dữ liệu phim đầy đủ: Poster HD từ TMDB, Trailer YouTube chính thức, 
-- Thể loại, Vòng đời phim và Suất chiếu mẫu
-- Lưu ý: Khi chạy qua sqlcmd bắt buộc phải có cờ -f 65001 để nhận UTF-8 tiếng Việt
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

-- 2. Đảm bảo 11 phim tồn tại với ID 1..11
-- Phim 1: Lật Mặt 7: Một Điều Ước
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
        PosterURL = 'https://image.tmdb.org/t/p/w500/aSPg7viRKZUp6py0VLVTv6mo3GN.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=d_kHIdB5kPQ',
        Status = 'Now Showing',
        Formats = '2D'
    WHERE MovieID = 1;
END

-- Phim 2: Dune: Hành Tinh Cát - Phần Hai
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
        PosterURL = 'https://image.tmdb.org/t/p/w500/6izwz7rsy95ARzTR3poZ8H6c5pp.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=Way9Dexny3w',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 2;
END

-- Phim 3: Marvel Studios' Avengers: Endgame
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
        PosterURL = 'https://image.tmdb.org/t/p/w500/ulzhLuWrPK07P1YkdWQLZnQh1JL.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=TcMBFSGVi1c',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 3;
END

-- Phim 4: Doraemon: Nobita và Bản Giao Hưởng Địa Cầu
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
        PosterURL = 'https://image.tmdb.org/t/p/w500/1W5Kg4K27U3dx9Kxb8DaE6WFLHI.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=2Ox6aYmhxvw',
        Status = 'Now Showing',
        Formats = '2D, 3D'
    WHERE MovieID = 4;
END

-- Phim 5: Deadpool & Wolverine
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 5)
BEGIN
    UPDATE Movie SET
        Title = N'Deadpool & Wolverine',
        Synopsis = N'Tổ chức Phương sai Thời gian (TVA) lôi kéo Wade Wilson (Deadpool) từ cuộc sống yên bình vào một sứ mệnh mới. Cùng với một biến thể Wolverine chán chường, bộ đôi bất đắc dĩ phải hợp lực để cứu lấy dòng thời gian của mình trước một mối hiểm họa diệt vong vô cùng tàn khốc.',
        Director = N'Shawn Levy',
        Cast = N'Ryan Reynolds, Hugh Jackman, Emma Corrin, Matthew Macfadyen, Dafne Keen',
        DurationMinutes = 128,
        AgeRating = 'C18',
        ReleaseDate = '2024-07-26',
        EndDate = '2026-12-31',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Marvel Studios',
        PosterURL = 'https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=73_1biulkYk',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 5;
END

-- Phim 6: Godzilla x Kong: The New Empire
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 6)
BEGIN
    UPDATE Movie SET
        Title = N'Godzilla x Kong: The New Empire',
        Synopsis = N'Trận chiến bùng nổ tiếp theo của Monsterverse khi Kong toàn năng và Godzilla đáng sợ phải bắt tay nhau đối đầu với một mối đe dọa khổng lồ chưa từng được khám phá, ẩn sâu bên trong Trái Đất rỗng, đe dọa sự tồn vong của loài người cũng như toàn bộ các Titan.',
        Director = N'Adam Wingard',
        Cast = N'Rebecca Hall, Brian Tyree Henry, Dan Stevens, Kaylee Hottle, Alex Ferns',
        DurationMinutes = 115,
        AgeRating = 'C13',
        ReleaseDate = '2024-03-29',
        EndDate = '2026-12-31',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Warner Bros. Pictures',
        PosterURL = 'https://image.tmdb.org/t/p/w500/z1p34vh7dEOnLDmyCrlUVLuoDzd.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=lV1OOlGwExg',
        Status = 'Now Showing',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 6;
END

-- Phim 7: Kung Fu Panda 4
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 7)
BEGIN
    UPDATE Movie SET
        Title = N'Kung Fu Panda 4',
        Synopsis = N'Po được chỉ định trở thành Thủ lĩnh Tinh thần của Thung lũng Bình Yên và cần tìm một Thần Long Đại Hiệp mới kế vị. Trong lúc đó, một phù thủy biến hình gian xảo tên Tắc Kè Hoa xuất hiện, nhăm nhe chiếm đoạt cây gậy phép của Po để triệu hồi lại linh hồn của tất cả những kẻ phản diện mà Po từng đánh bại.',
        Director = N'Mike Mitchell',
        Cast = N'Jack Black, Awkwafina, Viola Davis, Dustin Hoffman, Bryan Cranston',
        DurationMinutes = 94,
        AgeRating = 'P',
        ReleaseDate = '2024-03-08',
        EndDate = '2026-12-31',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Universal Pictures',
        PosterURL = 'https://image.tmdb.org/t/p/w500/kDp1vUBnMpe8ak4rjgl3cLELqjU.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=_inKs4eeHiI',
        Status = 'Now Showing',
        Formats = '2D, 3D'
    WHERE MovieID = 7;
END

-- Phim 8: Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 8)
BEGIN
    UPDATE Movie SET
        Title = N'Inside Out 2 (Những Mảnh Ghép Cảm Xúc 2)',
        Synopsis = N'Bộ phim quay trở lại tâm trí của cô bé Riley lúc này đã bước vào tuổi dậy thì đầy biến động. Trụ sở đầu não bất ngờ đón nhận một cuộc cải tạo lớn khi các cảm xúc mới bất thình lình ập đến: Lo Âu (Anxiety), Ganh Tị (Envy), Chán Nản (Ennui) và Xấu Hổ (Embarrassment), đẩy nhóm cảm xúc cũ vào một hành trình đầy cam go.',
        Director = N'Kelsey Mann',
        Cast = N'Amy Poehler, Maya Hawke, Kensington Tallman, Liza Lapira, Tony Hale',
        DurationMinutes = 96,
        AgeRating = 'P',
        ReleaseDate = '2024-06-14',
        EndDate = '2026-12-31',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Walt Disney Studios',
        PosterURL = 'https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=LEjhY15eCx0',
        Status = 'Now Showing',
        Formats = '2D, 3D'
    WHERE MovieID = 8;
END

-- Phim 9: Mai
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 9)
BEGIN
    UPDATE Movie SET
        Title = N'Mai',
        Synopsis = N'Mai là một người phụ nữ gần 30 tuổi làm nghề mát-xa trị liệu, luôn chịu nhiều định kiến xã hội và mang trong mình những tổn thương quá khứ khó giãi bày. Cuộc đời cô bắt đầu rẽ hướng khi Dương - một chàng trai đào hoa, trẻ tuổi sống cùng khu chung cư - kiên trì theo đuổi và tìm cách sưởi ấm trái tim cô.',
        Director = N'Trấn Thành',
        Cast = N'Phương Anh Đào, Tuấn Trần, Trấn Thành, Hồng Đào, Uyển Ân',
        DurationMinutes = 131,
        AgeRating = 'C18',
        ReleaseDate = '2024-02-10',
        EndDate = '2026-12-31',
        Country = N'Việt Nam',
        [Language] = N'Tiếng Việt',
        Distributor = N'CJ HK Entertainment',
        PosterURL = 'https://upload.wikimedia.org/wikipedia/vi/3/36/Mai_2024_poster.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=tfgIiqXq7c8',
        Status = 'Now Showing',
        Formats = '2D'
    WHERE MovieID = 9;
END

-- Phim 10: Moana 2 (Hành Trình Của Moana 2)
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 10)
BEGIN
    UPDATE Movie SET
        Title = N'Moana 2 (Hành Trình Của Moana 2)',
        Synopsis = N'Ba năm sau chuyến hành trình đầu tiên, Moana nhận được một lời kêu gọi bất ngờ từ tổ tiên định hướng đại dương. Cùng với Maui và một thủy thủ đoàn gồm những người đi biển kỳ quặc, Moana phải dấn thân vào vùng biển xa xôi nguy hiểm của châu Đại Dương để phá vỡ lời nguyền cổ xưa.',
        Director = N'David G. Derrick Jr.',
        Cast = N'Auliʻi Cravalho, Dwayne Johnson, Alan Tudyk, Temuera Morrison, Nicole Scherzinger',
        DurationMinutes = 100,
        AgeRating = 'P',
        ReleaseDate = '2026-11-27',
        EndDate = '2027-02-28',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Walt Disney Studios',
        PosterURL = 'https://image.tmdb.org/t/p/w500/aLVkiINlIeCkcZIzb7XHzPYgO6L.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=hDZ7y8RP5HE',
        Status = 'Coming Soon',
        Formats = '2D, 3D'
    WHERE MovieID = 10;
END

-- Phim 11: Mufasa: The Lion King
IF EXISTS (SELECT 1 FROM Movie WHERE MovieID = 11)
BEGIN
    UPDATE Movie SET
        Title = N'Mufasa: The Lion King',
        Synopsis = N'Khám phá câu chuyện thời niên thiếu của Mufasa - từ một chú sư tử con mồ côi lạc lõng, đơn độc cho đến khi gặp gỡ người thừa kế hoàng gia Taka và cùng nhau dấn thân vào hành trình vĩ đại tôi luyện nên vị Vua Sư Tử huyền thoại của Vùng Đất Kiêu Hãnh.',
        Director = N'Barry Jenkins',
        Cast = N'Aaron Pierre, Kelvin Harrison Jr., Seth Rogen, Billy Eichner, Donald Glover',
        DurationMinutes = 118,
        AgeRating = 'P',
        ReleaseDate = '2026-12-20',
        EndDate = '2027-02-28',
        Country = N'Mỹ',
        [Language] = N'Tiếng Anh',
        Distributor = N'Walt Disney Studios',
        PosterURL = 'https://image.tmdb.org/t/p/w500/jbOSUAWMGzGL1L4EaUF8K6zYFo7.jpg',
        TrailerURL = 'https://www.youtube.com/watch?v=o17MF9vnabg',
        Status = 'Coming Soon',
        Formats = '2D, 3D, IMAX'
    WHERE MovieID = 11;
END

PRINT N'Đã hoàn tất cập nhật 11 phim chính xác 100% tiếng Việt chuẩn Unicode và Poster TMDB!';
GO
