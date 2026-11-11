-- Insert initial memberships
INSERT INTO memberships (name, description)
VALUES
    (
        'Air Miles',
        'A rewards program offering miles and benefits through participating partners.'
    ),
    (
        'CAA',
        'An automotive and travel membership offering discounts and benefits.'
    ),
    (
        'Visa',
        'Credit card membership benefits and exclusive offers.'
    ),
    (
        'Costco',
        'A warehouse membership offering member-only deals.'
    ),
    (
        'Scene+',
        'A rewards program offering entertainment, dining, and shopping benefits.'
    );

-- Insert initial perks corresponding to memberships
INSERT INTO perks (
    title,
    description,
    product_name,
    discount_type,
    discount_value,
    geographic_area,
    expiry_date,
    membership_id
)
VALUES
    (
        'Free Domestic Flight',
        'Redeem 10,000 Air Miles for one free domestic flight, subject to availability.',
        'Domestic Flights',
        'POINTS_REDEMPTION',
        10000,
        'Canada',
        DATE '2027-12-31',
        (SELECT id FROM memberships WHERE name = 'Air Miles')
    ),
    (
        'Movie Ticket Discount',
        'Receive 10% off movie tickets at participating cinemas.',
        'Movie Tickets',
        'PERCENTAGE',
        10,
        'Canada',
        DATE '2027-03-31',
        (SELECT id FROM memberships WHERE name = 'Visa')
    ),
    (
        'Online Purchase Discount',
        'Receive 5% off eligible online purchases when paying with an eligible Visa card.',
        'Online Purchases',
        'PERCENTAGE',
        5,
        'Canada',
        DATE '2027-08-31',
        (SELECT id FROM memberships WHERE name = 'Visa')
    ),
    (
        'Hotel Booking Discount',
        'Receive 15% off eligible hotel bookings through participating travel partners.',
        'Hotel Bookings',
        'PERCENTAGE',
        15,
        'Canada',
        DATE '2027-06-30',
        (SELECT id FROM memberships WHERE name = 'CAA')
    ),
    (
        'Rental Car Discount',
        'Receive 20% off eligible rental car bookings.',
        'Rental Cars',
        'PERCENTAGE',
        20,
        'Canada',
        DATE '2027-09-30',
        (SELECT id FROM memberships WHERE name = 'CAA')
    ),
    (
        'Electronics Discount',
        'Receive $50 off eligible electronics purchases of $500 or more.',
        'Electronics',
        'FIXED_AMOUNT',
        50,
        'Canada',
        DATE '2027-01-31',
        (SELECT id FROM memberships WHERE name = 'Costco')
    ),
    (
        'Free Movie Ticket',
        'Redeem 1,000 Scene+ points for one movie ticket at participating cinemas.',
        'Movie Tickets',
        'POINTS_REDEMPTION',
        1000,
        'Canada',
        DATE '2027-12-31',
        (SELECT id FROM memberships WHERE name = 'Scene+')
    ),
    (
        'Restaurant Discount',
        'Receive 10% off your bill at participating restaurants.',
        'Restaurant Meals',
        'PERCENTAGE',
        10,
        'Ontario',
        DATE '2027-04-30',
        (SELECT id FROM memberships WHERE name = 'Scene+')
    );
